package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.ProviderAccumulatorsDocument;
import com.zyozmen.products.adapter.out.mongodb.document.LoteDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProductDocument;
import com.zyozmen.products.adapter.out.mongodb.document.DailySettlementSettingsDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProviderDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProviderDailyMovementsDocument;
import com.zyozmen.products.adapter.out.mongodb.document.TransaccionProveedorDocument;
import com.zyozmen.products.domain.model.DailySettlementAccumulator;
import com.zyozmen.products.domain.model.DailySettlementDetail;
import com.zyozmen.products.domain.model.DailySettlementMovement;
import com.zyozmen.products.domain.exception.OrderConflictException;
import com.zyozmen.products.domain.port.out.DailySettlementRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.util.Date;
import java.util.Optional;
import java.util.List;
import java.util.Comparator;
import java.util.TreeMap;
import java.math.BigDecimal;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailySettlementMongoAdapter implements DailySettlementRepositoryPort {
    private final MongoTemplate template;

    @Override
    public Page<ProviderAccumulator> findByDate(LocalDate date, String providerId, Pageable pageable) {
        var filter = Criteria.where("date").is(date.toString());
        if (providerId != null) {
            filter.and("providerId").is(providerId);
        }
        var query = Query.query(filter);
        long total = template.count(query, ProviderAccumulatorsDocument.class);
        var accumulators = template.find(query.with(pageable), ProviderAccumulatorsDocument.class);
        if (accumulators.isEmpty()) {
            return new PageImpl<>(java.util.List.of(), pageable, total);
        }
        var providersQuery = Query.query(Criteria.where("id").in(accumulators.stream()
                .map(ProviderAccumulatorsDocument::providerId).toList()));
        providersQuery.fields().include("nombre").include("financiero.saldoPendiente");
        var providers = template.find(providersQuery, ProviderDocument.class).stream()
                .collect(Collectors.toMap(ProviderDocument::getId, Function.identity()));
        var content = accumulators.stream().map(a -> {
            var provider = providers.get(a.providerId());
            if (provider == null || provider.getFinanciero() == null) {
                throw new IllegalStateException("No existe el proveedor o su saldo para un acumulado diario");
            }
            return new ProviderAccumulator(new DailySettlementAccumulator(a.providerId(), LocalDate.parse(a.date()),
                    a.totalSalesCount(), a.totalPqrCount(), a.salesRecognizedAmount(), a.pqrRecognizedAmount(),
                    a.reversedAmount(), a.linkedPaymentsAmount()), provider.getNombre(),
                    provider.getFinanciero().getSaldoPendiente());
        }).toList();
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<DailySettlementDetail> findDetail(LocalDate date, String providerId, Pageable pageable) {
        var providerObjectId = new ObjectId(providerId);
        var movementMatch = new Document("$match", new Document("providerId", providerObjectId)
                .append("date", date.toString()));
        var movementProjection = new Document("$project", new Document("_id", 0)
                .append("id", new Document("$toString", "$_id"))
                .append("movementType", 1)
                .append("providerId", new Document("$toString", "$providerId"))
                .append("orderId", 1)
                .append("itemIndex", 1)
                .append("lotId", new Document("$toString", "$lotId"))
                .append("containerId", new Document("$toString", "$containerId"))
                .append("strainId", 1)
                .append("presentationId", new Document("$toString", "$presentationId"))
                .append("quantity", 1)
                .append("contentPerUnit", 1)
                .append("baseUnit", 1)
                .append("baseUnitCost", 1)
                .append("unitCost", 1)
                .append("amount", 1)
                .append("timestamp", 1)
                .append("date", 1)
                .append("originalMovementId", new Document("$toString", "$originalMovementId"))
                .append("correlationId", new Document("$literal", null))
                .append("paymentType", new Document("$literal", null))
                .append("observations", new Document("$literal", null))
                .append("allocations", new Document("$literal", List.of())));
        var paymentPipeline = List.of(
                new Document("$match", new Document("proveedorId", providerObjectId)
                        .append("fechaLiquidacion", date.toString())
                        .append("correlationId", new Document("$type", "string"))),
                new Document("$sort", new Document("_id", 1)),
                new Document("$group", new Document("_id", "$correlationId")
                        .append("providerId", new Document("$first", "$proveedorId"))
                        .append("date", new Document("$first", "$fechaLiquidacion"))
                        .append("timestamp", new Document("$min", "$fecha"))
                        .append("paymentType", new Document("$first", "$tipo"))
                        .append("amount", new Document("$sum", "$monto"))
                        .append("observations", new Document("$first", "$observaciones"))
                        .append("allocations", new Document("$push", new Document(
                                "transactionId", new Document("$toString", "$_id"))
                                .append("lotId", new Document("$toString", "$loteId"))
                                .append("amount", "$monto")))),
                new Document("$project", new Document("_id", 0)
                        .append("id", new Document("$concat", List.of("payment:", "$_id")))
                        .append("movementType", new Document("$literal", "PAYMENT"))
                        .append("providerId", new Document("$toString", "$providerId"))
                        .append("date", 1)
                        .append("timestamp", 1)
                        .append("amount", 1)
                        .append("correlationId", "$_id")
                        .append("paymentType", 1)
                        .append("observations", 1)
                        .append("allocations", 1)));
        var union = new Document("$unionWith", new Document("coll",
                template.getCollectionName(TransaccionProveedorDocument.class))
                .append("pipeline", paymentPipeline));
        var facet = new Document("$facet", new Document("content", List.of(
                new Document("$skip", pageable.getOffset()),
                new Document("$limit", pageable.getPageSize())))
                .append("metadata", List.of(new Document("$count", "totalElements"))));
        var pipeline = List.of(movementMatch, movementProjection, union,
                new Document("$sort", new Document("timestamp", 1).append("id", 1)), facet);
        var result = template.getCollection(template.getCollectionName(ProviderDailyMovementsDocument.class))
                .aggregate(pipeline, Document.class).first();
        if (result == null) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        var sources = result.getList("content", Document.class, List.of());
        var lotIds = new HashSet<String>();
        var productIds = new HashSet<String>();
        for (var source : sources) {
            if (source.getString("strainId") != null) {
                productIds.add(source.getString("strainId"));
            }
            if (source.getString("lotId") != null) {
                lotIds.add(source.getString("lotId"));
            }
            for (var allocation : source.getList("allocations", Document.class, List.of())) {
                if (allocation.getString("lotId") != null) {
                    lotIds.add(allocation.getString("lotId"));
                }
            }
        }
        Map<String, LoteDocument> lots = new java.util.HashMap<>();
        if (!lotIds.isEmpty()) {
            var lotsQuery = Query.query(Criteria.where("id").in(lotIds));
            lotsQuery.fields().include("codigoLote").include("contenedores.contenedorId")
                    .include("contenedores.codigoContenedor").include("contenedores.presentaciones");
            lots = template.find(lotsQuery, LoteDocument.class).stream()
                    .collect(Collectors.toMap(LoteDocument::getId, Function.identity()));
        }
        Map<String, ProductDocument> products = new java.util.HashMap<>();
        if (!productIds.isEmpty()) {
            var productsQuery = Query.query(Criteria.where("id").in(productIds));
            productsQuery.fields().include("nombre");
            products = template.find(productsQuery, ProductDocument.class).stream()
                    .collect(Collectors.toMap(ProductDocument::id, Function.identity()));
        }
        String providerName = null;
        if (!sources.isEmpty()) {
            var providerQuery = Query.query(Criteria.where("id").is(providerId));
            providerQuery.fields().include("nombre");
            var providers = template.find(providerQuery, ProviderDocument.class);
            if (!providers.isEmpty()) {
                providerName = providers.getFirst().getNombre();
            } else {
                log.warn("No se encontro el proveedor {} al consultar el detalle diario", providerId);
            }
        }
        var entries = new ArrayList<DailySettlementDetail>();
        for (var source : sources) {
            entries.add(toDetail(source, providerName, lots, products));
        }
        var metadata = result.getList("metadata", Document.class, List.of());
        long total = metadata.isEmpty() ? 0 : metadata.getFirst().get("totalElements", Number.class).longValue();
        return new PageImpl<>(entries, pageable, total);
    }

    private DailySettlementDetail toDetail(Document source, String providerName, Map<String, LoteDocument> lots,
                                           Map<String, ProductDocument> products) {
        var allocations = new ArrayList<DailySettlementDetail.PaymentAllocation>();
        for (Document allocation : source.getList("allocations", Document.class, List.of())) {
            var lot = lots.get(allocation.getString("lotId"));
            if (lot == null && allocation.getString("lotId") != null) {
                log.warn("No se encontro el lote {} de una asignacion de pago", allocation.getString("lotId"));
            }
            allocations.add(new DailySettlementDetail.PaymentAllocation(
                    allocation.getString("transactionId"), allocation.getString("lotId"),
                    decimal(allocation.get("amount")), lot == null ? null : lot.getCodigoLote()));
        }
        var lot = lots.get(source.getString("lotId"));
        LoteDocument.ContenedorDocument container = null;
        if (lot != null && lot.getContenedores() != null) {
            container = lot.getContenedores().stream()
                    .filter(c -> java.util.Objects.equals(c.getId(), source.getString("containerId")))
                    .findFirst().orElse(null);
        }
        String presentationName = null;
        if (container != null && container.getPresentaciones() != null) {
            presentationName = container.getPresentaciones().stream()
                    .filter(p -> java.util.Objects.equals(p.presentacionId(), source.getString("presentationId")))
                    .map(LoteDocument.PresentacionInventarioDocument::nombre)
                    .filter(java.util.Objects::nonNull).findFirst().orElse(null);
        }
        if ((source.getString("lotId") != null && lot == null)
                || (source.getString("containerId") != null && container == null)
                || (source.getString("presentationId") != null && presentationName == null)) {
            log.warn("Referencias de inventario no disponibles para el movimiento diario {}", source.getString("id"));
        }
        var product = products.get(source.getString("strainId"));
        if (source.getString("strainId") != null && product == null) {
            log.warn("No se encontro el producto {} del movimiento diario {}",
                    source.getString("strainId"), source.getString("id"));
        }
        return new DailySettlementDetail(source.getString("id"), source.getString("movementType"),
                source.getString("providerId"), longValue(source.get("orderId")),
                integerValue(source.get("itemIndex")), source.getString("lotId"),
                source.getString("containerId"), source.getString("strainId"),
                source.getString("presentationId"), integerValue(source.get("quantity")),
                decimal(source.get("contentPerUnit")), source.getString("baseUnit"),
                decimal(source.get("baseUnitCost")), decimal(source.get("unitCost")),
                decimal(source.get("amount")), instant(source.get("timestamp")),
                LocalDate.parse(source.getString("date")), source.getString("originalMovementId"),
                source.getString("correlationId"), source.getString("paymentType"),
                source.getString("observations"), allocations, providerName,
                lot == null ? null : lot.getCodigoLote(),
                container == null ? null : container.getCodigoContenedor(), presentationName,
                product == null ? null : product.nombre());
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Decimal128 decimal128) {
            return decimal128.bigDecimalValue();
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        throw new IllegalStateException("El detalle de liquidacion contiene un importe invalido");
    }

    private Long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private Integer integerValue(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private Instant instant(Object value) {
        if (value instanceof Date timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof Instant timestamp) {
            return timestamp;
        }
        throw new IllegalStateException("El detalle de liquidacion contiene una marca de tiempo invalida");
    }

    @Override
    public Optional<Instant> activationAt() {
        var settings = template.findById(DailySettlementSettingsDocument.ID, DailySettlementSettingsDocument.class);
        if (settings == null) {
            throw new IllegalStateException("No se ha inicializado la configuracion de liquidacion diaria");
        }
        return Optional.ofNullable(settings.activatedAt());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recognizeSales(List<DailySettlementMovement> movements) {
        recognize(movements, false);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recognizePqrs(List<DailySettlementMovement> movements) {
        recognize(movements, true);
    }

    private void recognize(List<DailySettlementMovement> movements, boolean pqr) {
        if (movements.isEmpty()) {
            throw new IllegalArgumentException("El reconocimiento requiere movimientos");
        }
        var sorted = movements.stream().sorted(Comparator.comparing(DailySettlementMovement::providerId)
                .thenComparingInt(DailySettlementMovement::itemIndex)).toList();
        try {
            template.insert(sorted.stream().map(m -> new ProviderDailyMovementsDocument(null, m.orderId(),
                    m.itemIndex(), m.movementType(), m.providerId(), m.lotId(), m.containerId(), m.strainId(),
                    m.presentationId(), m.quantity(), m.contentPerUnit(), m.baseUnit(), m.baseUnitCost(),
                    m.unitCost(), m.amount(), m.timestamp(), m.date().toString(), null)).toList(),
                    ProviderDailyMovementsDocument.class);
            var providers = new TreeMap<String, List<DailySettlementMovement>>();
            for (var movement : sorted) {
                providers.computeIfAbsent(movement.providerId(), id -> new java.util.ArrayList<>()).add(movement);
            }
            for (var entry : providers.entrySet()) {
                var byDate = entry.getValue().stream().collect(Collectors.groupingBy(
                        DailySettlementMovement::date, TreeMap::new, Collectors.toList()));
                for (var day : byDate.entrySet()) {
                    long count = day.getValue().stream().mapToLong(DailySettlementMovement::quantity).sum();
                    BigDecimal amount = day.getValue().stream().map(DailySettlementMovement::amount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    var filter = Query.query(Criteria.where("providerId").is(entry.getKey())
                            .and("date").is(day.getKey().toString()));
                    var update = new Update().inc(pqr ? "totalPqrCount" : "totalSalesCount", count)
                            .inc(pqr ? "pqrRecognizedAmount" : "salesRecognizedAmount", amount)
                            .setOnInsert(pqr ? "totalSalesCount" : "totalPqrCount", 0L)
                            .setOnInsert(pqr ? "salesRecognizedAmount" : "pqrRecognizedAmount", BigDecimal.ZERO)
                            .setOnInsert("reversedAmount", BigDecimal.ZERO)
                            .setOnInsert("linkedPaymentsAmount", BigDecimal.ZERO);
                    template.upsert(filter, update, ProviderAccumulatorsDocument.class);
                }
            }
        } catch (DuplicateKeyException exception) {
            throw new OrderConflictException("La orden ya fue reconocida o el acumulado cambio; consulta la orden antes de reintentar");
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarPagoJornada(String providerId, LocalDate date, BigDecimal amount) {
        var filter = Query.query(Criteria.where("providerId").is(providerId).and("date").is(date.toString()));
        template.upsert(filter, new Update().inc("linkedPaymentsAmount", amount)
                .setOnInsert("totalSalesCount", 0L)
                .setOnInsert("totalPqrCount", 0L)
                .setOnInsert("salesRecognizedAmount", BigDecimal.ZERO)
                .setOnInsert("pqrRecognizedAmount", BigDecimal.ZERO)
                .setOnInsert("reversedAmount", BigDecimal.ZERO), ProviderAccumulatorsDocument.class);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void reversePqr(Long orderId, Instant cancelledAt) {
        var originals = template.find(Query.query(Criteria.where("orderId").is(orderId)
                        .and("movementType").is("PQR")), ProviderDailyMovementsDocument.class);
        // Las PQR historicas sin reconocimiento no tienen costo financiero que revertir.
        if (originals.isEmpty()) {
            return;
        }
        if (originals.stream().anyMatch(m -> cancelledAt.isBefore(m.timestamp()))) {
            throw new OrderConflictException("La cancelacion no puede ser anterior al reconocimiento de la PQR");
        }
        try {
            template.insert(originals.stream().map(m -> new ProviderDailyMovementsDocument(null, m.orderId(),
                    m.itemIndex(), "PQR_REVERSAL", m.providerId(), m.lotId(), m.containerId(), m.strainId(),
                    m.presentationId(), m.quantity(), m.contentPerUnit(), m.baseUnit(), m.baseUnitCost(),
                    m.unitCost(), m.amount(), cancelledAt, m.date(), m.id())).toList(),
                    ProviderDailyMovementsDocument.class);
            var byProvider = originals.stream().collect(Collectors.groupingBy(
                    ProviderDailyMovementsDocument::providerId, TreeMap::new, Collectors.toList()));
            for (var provider : byProvider.entrySet()) {
                var byDate = provider.getValue().stream().collect(Collectors.groupingBy(
                        ProviderDailyMovementsDocument::date, TreeMap::new, Collectors.toList()));
                for (var day : byDate.entrySet()) {
                    long count = day.getValue().stream().mapToLong(ProviderDailyMovementsDocument::quantity).sum();
                    BigDecimal amount = day.getValue().stream().map(ProviderDailyMovementsDocument::amount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    var filter = Query.query(Criteria.where("providerId").is(provider.getKey())
                            .and("date").is(day.getKey()).and("totalPqrCount").gte(count));
                    filter.addCriteria(Criteria.expr(() -> new org.bson.Document("$gte", List.of(
                            new org.bson.Document("$subtract", List.of("$pqrRecognizedAmount", "$reversedAmount")),
                            new org.bson.types.Decimal128(amount)))));
                    var result = template.updateFirst(filter, new Update().inc("totalPqrCount", -count)
                            .inc("reversedAmount", amount), ProviderAccumulatorsDocument.class);
                    if (result.getModifiedCount() != 1) {
                        throw new OrderConflictException("El acumulado original de la PQR no permite esta reversion");
                    }
                }
            }
        } catch (DuplicateKeyException exception) {
            throw new OrderConflictException("La PQR ya fue revertida; consulta la orden antes de reintentar");
        }
    }
}

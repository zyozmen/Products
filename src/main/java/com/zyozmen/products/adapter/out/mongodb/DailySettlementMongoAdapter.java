package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.ProviderAccumulatorsDocument;
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
        var entries = result.getList("content", Document.class, List.of()).stream()
                .map(this::toDetail).toList();
        var metadata = result.getList("metadata", Document.class, List.of());
        long total = metadata.isEmpty() ? 0 : metadata.getFirst().get("totalElements", Number.class).longValue();
        return new PageImpl<>(entries, pageable, total);
    }

    private DailySettlementDetail toDetail(Document source) {
        var allocations = new ArrayList<DailySettlementDetail.PaymentAllocation>();
        for (Document allocation : source.getList("allocations", Document.class, List.of())) {
            allocations.add(new DailySettlementDetail.PaymentAllocation(
                    allocation.getString("transactionId"), allocation.getString("lotId"),
                    decimal(allocation.get("amount"))));
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
                source.getString("observations"), allocations);
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

package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.ProviderAccumulatorsDocument;
import com.zyozmen.products.adapter.out.mongodb.document.DailySettlementSettingsDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProviderDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProviderDailyMovementsDocument;
import com.zyozmen.products.domain.model.DailySettlementAccumulator;
import com.zyozmen.products.domain.model.DailySettlementMovement;
import com.zyozmen.products.domain.exception.OrderConflictException;
import com.zyozmen.products.domain.port.out.DailySettlementRepositoryPort;
import lombok.RequiredArgsConstructor;
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

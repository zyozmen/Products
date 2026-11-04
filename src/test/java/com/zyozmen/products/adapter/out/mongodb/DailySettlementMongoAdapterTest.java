package com.zyozmen.products.adapter.out.mongodb;

import com.mongodb.client.result.UpdateResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.AggregateIterable;
import com.zyozmen.products.adapter.out.mongodb.document.DailySettlementSettingsDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProviderAccumulatorsDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProviderDailyMovementsDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProviderDocument;
import com.zyozmen.products.adapter.out.mongodb.document.TransaccionProveedorDocument;
import com.zyozmen.products.domain.exception.OrderConflictException;
import com.zyozmen.products.domain.model.DailySettlementDetail;
import com.zyozmen.products.domain.model.DailySettlementMovement;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailySettlementMongoAdapterTest {

    private final MongoTemplate template = mock(MongoTemplate.class);
    private final DailySettlementMongoAdapter adapter = new DailySettlementMongoAdapter(template);
    private final LocalDate date = LocalDate.of(2026, 11, 3);
    private final Instant timestamp = Instant.parse("2026-11-03T15:00:00Z");

    @Test
    void findsEmptyOrPopulatedProviderPagesAndRequiresProviderBalance() {
        var page = PageRequest.of(0, 10);
        when(template.count(any(Query.class), eq(ProviderAccumulatorsDocument.class))).thenReturn(0L, 1L);
        when(template.find(any(Query.class), eq(ProviderAccumulatorsDocument.class))).thenReturn(
                List.of(),
                List.of(new ProviderAccumulatorsDocument("a1", "provider-1", date.toString(),
                        2, 1, new BigDecimal("20.00"), new BigDecimal("10.00"),
                        BigDecimal.ZERO, BigDecimal.ZERO)));
        var provider = ProviderDocument.builder().id("provider-1").nombre("Proveedor")
                .financiero(new ProviderDocument.FinancieroDocument(
                        BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.valueOf(100))).build();
        when(template.find(any(Query.class), eq(ProviderDocument.class))).thenReturn(List.of(provider));

        assertThat(adapter.findByDate(date, null, page)).isEmpty();
        var result = adapter.findByDate(date, "provider-1", page);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().providerName()).isEqualTo("Proveedor");
        assertThat(result.getContent().getFirst().providerOutstandingBalance()).isEqualByComparingTo("100");
        assertThat(result.getContent().getFirst().accumulator().totalSalesCount()).isEqualTo(2);

        when(template.find(any(Query.class), eq(ProviderAccumulatorsDocument.class))).thenReturn(
                List.of(new ProviderAccumulatorsDocument("a1", "missing", date.toString(),
                        0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)));
        when(template.find(any(Query.class), eq(ProviderDocument.class))).thenReturn(List.of());
        assertThatThrownBy(() -> adapter.findByDate(date, null, page))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mapsPaginatedDetailIncludingGroupedPaymentAllocations() {
        @SuppressWarnings("unchecked")
        MongoCollection<Document> collection = mock(MongoCollection.class);
        @SuppressWarnings("unchecked")
        AggregateIterable<Document> aggregation = mock(AggregateIterable.class);
        when(template.getCollectionName(ProviderDailyMovementsDocument.class))
                .thenReturn("ProviderDailyMovements");
        when(template.getCollectionName(TransaccionProveedorDocument.class))
                .thenReturn("Provider_Transactions");
        when(template.getCollection("ProviderDailyMovements")).thenReturn(collection);
        when(collection.aggregate(anyList(), eq(Document.class))).thenReturn(aggregation);
        var payment = new Document("id", "payment:correlation-1")
                .append("movementType", "PAYMENT")
                .append("providerId", "6500000000000000000000ab")
                .append("date", date.toString())
                .append("timestamp", Date.from(timestamp))
                .append("amount", new Decimal128(new BigDecimal("20.00")))
                .append("correlationId", "correlation-1")
                .append("paymentType", "ABONO")
                .append("allocations", List.of(new Document("transactionId", "transaction-1")
                        .append("lotId", "650000000000000000000201")
                        .append("amount", new Decimal128(new BigDecimal("20.00")))));
        when(aggregation.first()).thenReturn(new Document("content", List.of(payment))
                .append("metadata", List.of(new Document("totalElements", 3L))));

        var page = adapter.findDetail(date, "6500000000000000000000ab", PageRequest.of(1, 1));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(3);
        DailySettlementDetail detail = page.getContent().getFirst();
        assertThat(detail.movementType()).isEqualTo("PAYMENT");
        assertThat(detail.amount()).isEqualByComparingTo("20.00");
        assertThat(detail.unitCost()).isNull();
        assertThat(detail.allocations()).containsExactly(new DailySettlementDetail.PaymentAllocation(
                "transaction-1", "650000000000000000000201", new BigDecimal("20.00")));

        @SuppressWarnings("unchecked")
        var pipelineCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(collection).aggregate(pipelineCaptor.capture(), eq(Document.class));
        var pipeline = (List<Document>) pipelineCaptor.getValue();
        assertThat(pipeline.get(2)).containsKey("$unionWith");
        assertThat((Document) pipeline.get(3).get("$sort"))
                .containsEntry("timestamp", 1).containsEntry("id", 1);
        assertThat((List<Document>) ((Document) pipeline.get(4).get("$facet")).get("content"))
                .containsExactly(new Document("$skip", 1L), new Document("$limit", 1));
    }

    @Test
    void readsActivationTimestampAndRejectsMissingSettings() {
        when(template.findById(DailySettlementSettingsDocument.ID, DailySettlementSettingsDocument.class))
                .thenReturn(new DailySettlementSettingsDocument(DailySettlementSettingsDocument.ID, timestamp), null);

        assertThat(adapter.activationAt()).isEqualTo(Optional.of(timestamp));
        assertThatThrownBy(adapter::activationAt)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se ha inicializado");
    }

    @Test
    void recognizesSalesAndPqrsInStableProviderOrder() {
        var laterProvider = movement("z-provider", 0, "SALE", 2, "12.00");
        var earlierProvider = movement("a-provider", 1, "SALE", 3, "15.00");

        adapter.recognizeSales(List.of(laterProvider, earlierProvider));
        var inserted = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(template).insert(inserted.capture(), eq(ProviderDailyMovementsDocument.class));
        assertThat((List<ProviderDailyMovementsDocument>) inserted.getValue())
                .extracting(ProviderDailyMovementsDocument::providerId)
                .containsExactly("a-provider", "z-provider");
        verify(template, org.mockito.Mockito.times(2)).upsert(any(Query.class), any(), eq(ProviderAccumulatorsDocument.class));

        adapter.recognizePqrs(List.of(movement("a-provider", 2, "PQR", 1, "8.00")));
        verify(template, org.mockito.Mockito.times(2)).insert(anyList(), eq(ProviderDailyMovementsDocument.class));
        verify(template, org.mockito.Mockito.times(3)).upsert(any(Query.class), any(), eq(ProviderAccumulatorsDocument.class));
    }

    @Test
    void rejectsEmptyOrDuplicateRecognition() {
        assertThatThrownBy(() -> adapter.recognizeSales(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        when(template.insert(anyList(), eq(ProviderDailyMovementsDocument.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));

        assertThatThrownBy(() -> adapter.recognizePqrs(List.of(movement("provider-1", 0, "PQR", 1, "8.00"))))
                .isInstanceOf(OrderConflictException.class);
        verify(template, never()).upsert(any(Query.class), any(), eq(ProviderAccumulatorsDocument.class));
    }

    @Test
    void reversesRecognizedPqrAndPreservesOriginalMovementLink() {
        var original = new ProviderDailyMovementsDocument("movement-1", 42L, 0, "PQR",
                "provider-1", "lot-1", "container-1", "strain-1", "presentation-1",
                2, BigDecimal.TEN, "GRAMOS", BigDecimal.ONE, BigDecimal.valueOf(5),
                BigDecimal.TEN, timestamp, date.toString(), null);
        when(template.find(any(Query.class), eq(ProviderDailyMovementsDocument.class))).thenReturn(
                List.of(), List.of(original), List.of(original));
        when(template.updateFirst(any(Query.class), any(), eq(ProviderAccumulatorsDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null), UpdateResult.acknowledged(0, 0L, null));

        adapter.reversePqr(42L, timestamp.minusSeconds(1));
        verify(template, never()).updateFirst(any(Query.class), any(), eq(ProviderAccumulatorsDocument.class));
        assertThatThrownBy(() -> adapter.reversePqr(42L, timestamp.minusSeconds(1)))
                .isInstanceOf(OrderConflictException.class)
                .hasMessageContaining("anterior");

        adapter.reversePqr(42L, timestamp.plusSeconds(60));
        var inserted = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(template).insert(inserted.capture(), eq(ProviderDailyMovementsDocument.class));
        var reversal = ((List<ProviderDailyMovementsDocument>) inserted.getValue()).getFirst();
        assertThat(reversal.movementType()).isEqualTo("PQR_REVERSAL");
        assertThat(reversal.originalMovementId()).isEqualTo("movement-1");
        assertThat(reversal.timestamp()).isEqualTo(timestamp.plusSeconds(60));

        assertThatThrownBy(() -> adapter.reversePqr(42L, timestamp.plusSeconds(60)))
                .isInstanceOf(OrderConflictException.class)
                .hasMessageContaining("no permite esta reversion");
    }

    @Test
    void mapsDuplicatePqrReversalToConflict() {
        var original = new ProviderDailyMovementsDocument("movement-1", 42L, 0, "PQR",
                "provider-1", "lot-1", "container-1", "strain-1", "presentation-1",
                1, BigDecimal.TEN, "GRAMOS", BigDecimal.ONE, BigDecimal.TEN,
                BigDecimal.TEN, timestamp, date.toString(), null);
        when(template.find(any(Query.class), eq(ProviderDailyMovementsDocument.class))).thenReturn(List.of(original));
        when(template.insert(anyList(), eq(ProviderDailyMovementsDocument.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));

        assertThatThrownBy(() -> adapter.reversePqr(42L, timestamp.plusSeconds(1)))
                .isInstanceOf(OrderConflictException.class)
                .hasMessageContaining("ya fue revertida");
    }

    private DailySettlementMovement movement(String providerId, int itemIndex, String type,
                                             int quantity, String amount) {
        return new DailySettlementMovement(42L, itemIndex, type, providerId,
                "lot-1", "container-1", "strain-1", "presentation-1",
                quantity, BigDecimal.TEN, "GRAMOS", BigDecimal.ONE,
                BigDecimal.valueOf(5), new BigDecimal(amount), timestamp, date);
    }
}

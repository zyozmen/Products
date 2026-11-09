package com.zyozmen.products.adapter.out.mongodb;

import com.mongodb.client.result.UpdateResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.AggregateIterable;
import com.zyozmen.products.adapter.out.mongodb.document.DailySettlementSettingsDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProviderAccumulatorsDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProviderDailyMovementsDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProviderDocument;
import com.zyozmen.products.adapter.out.mongodb.document.LoteDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProductDocument;
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
    void enrichesMovementsAndPaymentAllocationsWithNamesUsingBatchedQueries() throws Exception {
        @SuppressWarnings("unchecked")
        MongoCollection<Document> collection = mock(MongoCollection.class);
        @SuppressWarnings("unchecked")
        AggregateIterable<Document> aggregation = mock(AggregateIterable.class);
        when(template.getCollectionName(ProviderDailyMovementsDocument.class)).thenReturn("movements");
        when(template.getCollectionName(TransaccionProveedorDocument.class)).thenReturn("payments");
        when(template.getCollection("movements")).thenReturn(collection);
        when(collection.aggregate(anyList(), eq(Document.class))).thenReturn(aggregation);
        String providerId = "6500000000000000000000ab";
        String lotId = "650000000000000000000201";
        String containerId = "650000000000000000000301";
        String presentationId = "650000000000000000000401";
        String productId = "650000000000000000000501";
        var sale = new Document("id", "sale").append("movementType", "SALE")
                .append("providerId", providerId).append("lotId", lotId).append("containerId", containerId)
                .append("presentationId", presentationId).append("strainId", productId)
                .append("date", date.toString())
                .append("timestamp", Date.from(timestamp));
        var payment = new Document("id", "payment:1").append("movementType", "PAYMENT")
                .append("providerId", providerId).append("date", date.toString())
                .append("timestamp", Date.from(timestamp))
                .append("allocations", List.of(new Document("transactionId", "t1").append("lotId", lotId)
                        .append("amount", new Decimal128(BigDecimal.TEN))));
        when(aggregation.first()).thenReturn(new Document("content", List.of(sale, payment))
                .append("metadata", List.of(new Document("totalElements", 2L))));
        var presentation = new LoteDocument.PresentacionInventarioDocument(presentationId, "SKU", "Bolsa 5g",
                BigDecimal.valueOf(5), 10, 5, BigDecimal.ONE, BigDecimal.TEN, true, false, null);
        var container = LoteDocument.ContenedorDocument.builder().id(containerId).codigoContenedor("CONT-001")
                .presentaciones(List.of(presentation)).build();
        var context = new org.springframework.data.mongodb.core.mapping.MongoMappingContext();
        var conversions = org.springframework.data.mongodb.core.convert.MongoCustomConversions.create(adapter -> { });
        context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        context.afterPropertiesSet();
        var converter = new org.springframework.data.mongodb.core.convert.MappingMongoConverter(
                org.springframework.data.mongodb.core.convert.NoOpDbRefResolver.INSTANCE, context);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
        var storedPresentation = new Document("presentacionId", new org.bson.types.ObjectId(presentationId))
                .append("sku", presentation.sku()).append("nombre", presentation.nombre())
                .append("contenidoUnidad", "5").append("unidadesCreadas", 10).append("stockDisponible", 5)
                .append("costoPresentacionCalculado", "1").append("precioVenta", "10")
                .append("activoMarketplace", true).append("confirmacionMargenNegativo", false);
        when(template.find(any(Query.class), eq(LoteDocument.class))).thenAnswer(invocation -> {
            Query query = invocation.getArgument(0);
            assertThat(query.getFieldsObject())
                    .containsEntry("contenedores.presentaciones", 1)
                    .doesNotContainKeys("contenedores.presentaciones.presentacionId",
                            "contenedores.presentaciones.nombre");
            var projectedLot = new Document("_id", new org.bson.types.ObjectId(lotId))
                    .append("codigoLote", "LOT-001")
                    .append("contenedores", List.of(
                            new Document("contenedorId", new org.bson.types.ObjectId(container.getId()))
                                    .append("codigoContenedor", container.getCodigoContenedor())
                                    .append("presentaciones", List.of(storedPresentation))));
            return List.of(converter.read(LoteDocument.class, projectedLot));
        });
        when(template.find(any(Query.class), eq(ProviderDocument.class))).thenReturn(List.of(
                ProviderDocument.builder().id(providerId).nombre("Proveedor Verde").build()));
        when(template.find(any(Query.class), eq(ProductDocument.class))).thenReturn(List.of(
                new ProductDocument(productId, "Sativa", null, null, null, null, null)));

        var page = adapter.findDetail(date, providerId, PageRequest.of(0, 20));

        var detail = page.getContent().getFirst();
        assertThat(detail.providerName()).isEqualTo("Proveedor Verde");
        assertThat(detail.lotCode()).isEqualTo("LOT-001");
        assertThat(detail.containerCode()).isEqualTo("CONT-001");
        assertThat(detail.presentationName()).isEqualTo("Bolsa 5g");
        assertThat(detail.productName()).isEqualTo("Sativa");
        assertThat(detail.strainId()).isEqualTo(productId);
        assertThat(detail.providerId()).isEqualTo(providerId);
        assertThat(detail.lotId()).isEqualTo(lotId);
        assertThat(detail.containerId()).isEqualTo(containerId);
        assertThat(detail.presentationId()).isEqualTo(presentationId);
        var paymentDetail = page.getContent().getLast();
        assertThat(paymentDetail.providerName()).isEqualTo("Proveedor Verde");
        assertThat(paymentDetail.lotCode()).isNull();
        assertThat(paymentDetail.containerCode()).isNull();
        assertThat(paymentDetail.presentationName()).isNull();
        assertThat(paymentDetail.productName()).isNull();
        assertThat(paymentDetail.allocations().getFirst().lotCode()).isEqualTo("LOT-001");
        assertThat(paymentDetail.allocations().getFirst().lotId()).isEqualTo(lotId);
        verify(template).find(any(Query.class), eq(LoteDocument.class));
        verify(template).find(any(Query.class), eq(ProviderDocument.class));
        var productQuery = org.mockito.ArgumentCaptor.forClass(Query.class);
        verify(template).find(productQuery.capture(), eq(ProductDocument.class));
        assertThat(productQuery.getValue().getQueryObject())
                .isEqualTo(new Document("id", new Document("$in", java.util.Set.of(productId))));
        assertThat(productQuery.getValue().getFieldsObject()).isEqualTo(new Document("nombre", 1));

        when(template.find(any(Query.class), eq(LoteDocument.class))).thenReturn(List.of());
        when(template.find(any(Query.class), eq(ProviderDocument.class))).thenReturn(List.of());
        when(template.find(any(Query.class), eq(ProductDocument.class))).thenReturn(List.of());
        var historical = adapter.findDetail(date, providerId, PageRequest.of(0, 20)).getContent().getFirst();
        assertThat(historical.lotId()).isEqualTo(lotId);
        assertThat(historical.providerName()).isNull();
        assertThat(historical.lotCode()).isNull();
        assertThat(historical.containerCode()).isNull();
        assertThat(historical.presentationName()).isNull();
        assertThat(historical.productName()).isNull();
        assertThat(historical.strainId()).isEqualTo(productId);
    }

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
                "transaction-1", "650000000000000000000201", new BigDecimal("20.00"), null));

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

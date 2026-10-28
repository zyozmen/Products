package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.OrderProductException;
import com.zyozmen.products.domain.model.*;
import com.zyozmen.products.domain.port.out.DailySettlementRepositoryPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderSettlementServiceTest {
    private final DailySettlementRepositoryPort repository = mock(DailySettlementRepositoryPort.class);
    private final OrderSettlementService service = new OrderSettlementService(repository);
    private static final Instant START = Instant.parse("2026-10-23T05:00:00Z");

    @Test
    void multiplicaSnapshotPorCantidadYCadaItemTieneIdentidadEstableEnJornadaBogota() {
        when(repository.activationAt()).thenReturn(Optional.of(START));
        var order = order();
        order.setPaidAt(Instant.parse("2026-10-24T04:59:59Z"));
        order.setItems(List.of(item(3), item(2)));
        service.recognizeOrderMovement(order);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DailySettlementMovement>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).recognizeSales(captor.capture());
        var movements = captor.getValue();
        assertThat(movements).hasSize(2);
        assertThat(movements).extracting(DailySettlementMovement::itemIndex).containsExactly(0, 1);
        assertThat(movements).allSatisfy(m -> {
            assertThat(m.orderId()).isEqualTo(1L);
            assertThat(m.date()).isEqualTo(LocalDate.of(2026, 10, 23));
            assertThat(m.movementType()).isEqualTo("SALE");
            assertThat(m.unitCost()).isEqualByComparingTo("10000");
        });
        assertThat(movements.getFirst().amount()).isEqualByComparingTo("30000");
        assertThat(movements.getLast().amount()).isEqualByComparingTo("20000");
    }

    @Test
    void exactamenteEnElCorteEsElegibleYMedianocheCambiaJornada() {
        when(repository.activationAt()).thenReturn(Optional.of(START));
        var order = order();
        order.setCreatedAt(START);
        order.setPaidAt(Instant.parse("2026-10-24T05:00:00Z"));
        service.recognizeOrderMovement(order);
        verify(repository).recognizeSales(argThat(ms -> ms.getFirst().date().equals(LocalDate.of(2026, 10, 24))));
    }

    @Test
    void ordenAnteriorAlCorteNoSeAcumulaAunqueSeaPagadaDespues() {
        when(repository.activationAt()).thenReturn(Optional.of(START));
        var order = order();
        order.setCreatedAt(START.minusMillis(1));
        order.setItems(null);
        service.recognizeOrderMovement(order);
        verify(repository, never()).recognizeSales(any());
    }

    @Test
    void noReconocePqrContraEntregaNiOtrosEstados() {
        var pqr = order();
        pqr.setPqr(true);
        service.recognizeOrderMovement(pqr);
        var cod = order();
        cod.setCashOnDelivery(true);
        service.recognizeOrderMovement(cod);
        var created = order();
        created.setStatus(OrderStatus.CREADA);
        service.recognizeOrderMovement(created);
        verifyNoInteractions(repository);
    }

    @Test
    void faltaDeActivacionEsErrorExplicitoNoExclusionSilenciosa() {
        when(repository.activationAt()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.recognizeOrderMovement(order())).isInstanceOf(IllegalStateException.class);
        verify(repository, never()).recognizeSales(any());
    }

    @Test
    void rechazaOrdenSinIdentidadFechasOItems() {
        when(repository.activationAt()).thenReturn(Optional.of(START));
        var order = order();
        order.setCreatedAt(null);
        assertInvalid(order);
        order = order();
        order.setId(null);
        assertInvalid(order);
        order = order();
        order.setPaidAt(null);
        assertInvalid(order);
        order = order();
        order.setPaidAt(START.minusSeconds(1));
        assertInvalid(order);
        order = order();
        order.setItems(List.of());
        assertInvalid(order);
        verify(repository, never()).recognizeSales(any());
    }

    @Test
    void snapshotAusenteInconsistenteOCantidadInvalidaNoSeReemplazanPorCero() {
        when(repository.activationAt()).thenReturn(Optional.of(START));
        var order = order();
        order.getItems().getFirst().setTrazabilidad(null);
        assertInvalid(order);
        order = order();
        order.getItems().getFirst().setQuantity(0);
        assertInvalid(order);
        order = order();
        order.getItems().getFirst().setProductId("otro");
        assertInvalid(order);
        var t = item(1).getTrazabilidad();
        order = order();
        order.getItems().getFirst().setTrazabilidad(new PresentationTrace(t.proveedorId(), t.loteId(),
                t.contenedorId(), t.cepaId(), t.presentacionId(), t.contenidoUnidad(), t.unidadBase(),
                t.costoBasePorUnidad(), new BigDecimal("1")));
        assertInvalid(order);
        verify(repository, never()).recognizeSales(any());
    }

    @Test
    void costoCeroEsValidoYSnapshotSeRedondeaComoEnCreacion() {
        when(repository.activationAt()).thenReturn(Optional.of(START));
        var order = order();
        var t = item(1).getTrazabilidad();
        order.getItems().getFirst().setTrazabilidad(new PresentationTrace(t.proveedorId(), t.loteId(),
                t.contenedorId(), t.cepaId(), t.presentacionId(), new BigDecimal("0.333"), t.unidadBase(),
                BigDecimal.ONE, new BigDecimal("0.33")));
        service.recognizeOrderMovement(order);
        verify(repository).recognizeSales(argThat(ms -> ms.getFirst().amount().compareTo(new BigDecimal("0.99")) == 0));
    }

    private void assertInvalid(Order order) {
        assertThatThrownBy(() -> service.recognizeOrderMovement(order)).isInstanceOf(OrderProductException.class);
    }

    @Test
    void contraEntregaReconoceEnDeliveredAtNoEnPaidAtIncluyendoMedianocheBogota() {
        when(repository.activationAt()).thenReturn(Optional.of(START));
        var order = order();
        order.setCashOnDelivery(true);
        order.setStatus(OrderStatus.ENTREGADA);
        for (var instant : List.of(Instant.parse("2026-10-24T04:59:59Z"), Instant.parse("2026-10-24T05:00:00Z"))) {
            order.setDeliveredAt(instant);
            service.recognizeOrderMovement(order);
            verify(repository).recognizeSales(argThat(ms -> ms.getFirst().timestamp().equals(instant)
                    && ms.getFirst().date().equals(instant.atZone(DailySettlementService.BUSINESS_ZONE).toLocalDate())
                    && ms.getFirst().amount().compareTo(new BigDecimal("30000")) == 0));
        }
    }

    @Test
    void contraEntregaAnteriorAlInicioSeExcluyeAunqueSeEntregueDespues() {
        when(repository.activationAt()).thenReturn(Optional.of(START));
        var order = order();
        order.setCashOnDelivery(true);
        order.setStatus(OrderStatus.ENTREGADA);
        order.setCreatedAt(START.minusMillis(1));
        order.setDeliveredAt(START.plusSeconds(100));
        service.recognizeOrderMovement(order);
        verify(repository, never()).recognizeSales(any());
    }

    @Test
    void entregaSinFechaValidaFallaYNoUsaPaidAtComoReemplazo() {
        when(repository.activationAt()).thenReturn(Optional.of(START));
        var order = order();
        order.setCashOnDelivery(true);
        order.setStatus(OrderStatus.ENTREGADA);
        assertInvalid(order);
        order.setDeliveredAt(START);
        assertInvalid(order);
        verify(repository, never()).recognizeSales(any());
    }

    @Test
    void ventaNormalEntregadaYPqrEntregadaNoReconocenCosto() {
        var order = order();
        order.setStatus(OrderStatus.ENTREGADA);
        service.recognizeOrderMovement(order);
        order.setCashOnDelivery(true);
        order.setPqr(true);
        service.recognizeOrderMovement(order);
        verifyNoInteractions(repository);
    }

    private Order order() {
        return Order.builder().id(1L).status(OrderStatus.PAGADA).createdAt(START.plusSeconds(1))
                .paidAt(START.plusSeconds(2)).items(List.of(item(3))).build();
    }

    @Test
    void pqrCreadaReconoceCantidadRealConFechaDeCreacionYSinVentaComercial() {
        when(repository.activationAt()).thenReturn(Optional.of(START));
        var pqr = order();
        pqr.setPqr(true);
        pqr.setStatus(OrderStatus.CREADA);
        pqr.setPaidAt(null);
        pqr.setCreatedAt(Instant.parse("2026-10-24T04:59:59Z"));
        service.recognizeOrderMovement(pqr);
        verify(repository).recognizePqrs(argThat(ms -> ms.size() == 1
                && ms.getFirst().movementType().equals("PQR")
                && ms.getFirst().quantity() == 3
                && ms.getFirst().amount().compareTo(new BigDecimal("30000")) == 0
                && ms.getFirst().timestamp().equals(pqr.getCreatedAt())
                && ms.getFirst().date().equals(LocalDate.of(2026, 10, 23))));
        verify(repository, never()).recognizeSales(any());
    }

    @Test
    void pqrAnteriorAlCorteNoSeReconoce() {
        when(repository.activationAt()).thenReturn(Optional.of(START));
        var pqr = order();
        pqr.setPqr(true);
        pqr.setStatus(OrderStatus.CREADA);
        pqr.setCreatedAt(START.minusMillis(1));
        service.recognizeOrderMovement(pqr);
        verify(repository, never()).recognizePqrs(any());
    }

    @Test
    void cancelacionUsaIdentidadOriginalNoCostoActualNiFechaDeCreacionParaRevertir() {
        var pqr = order();
        pqr.setPqr(true);
        pqr.setStatus(OrderStatus.CANCELADA);
        pqr.setCancelledAt(Instant.parse("2026-10-26T18:00:00Z"));
        pqr.setItems(null);
        pqr.setCreatedAt(null);
        service.recognizeOrderMovement(pqr);
        verify(repository).reversePqr(pqr.getId(), pqr.getCancelledAt());
        verifyNoMoreInteractions(repository);
    }

    @Test
    void cancelacionSinIdOFechaEsErrorExplicito() {
        var pqr = order();
        pqr.setPqr(true);
        pqr.setStatus(OrderStatus.CANCELADA);
        assertInvalid(pqr);
        pqr.setCancelledAt(START);
        pqr.setId(null);
        assertInvalid(pqr);
        verifyNoInteractions(repository);
    }

    private OrderItem item(int quantity) {
        return OrderItem.builder().quantity(quantity).productId("650000000000000000000401")
                .trazabilidad(new PresentationTrace("650000000000000000000101", "650000000000000000000201",
                        "650000000000000000000301", "cepa", "650000000000000000000401",
                        BigDecimal.TEN, "GRAMOS", new BigDecimal("1000"), new BigDecimal("10000"))).build();
    }
}

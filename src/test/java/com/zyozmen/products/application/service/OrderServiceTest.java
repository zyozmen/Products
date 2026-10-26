package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.OrderConflictException;
import com.zyozmen.products.domain.exception.OrderProductException;
import com.zyozmen.products.domain.exception.OrderValidationException;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.Order;
import com.zyozmen.products.domain.model.OrderDelivery;
import com.zyozmen.products.domain.model.OrderItem;
import com.zyozmen.products.domain.model.OrderStatus;
import com.zyozmen.products.domain.model.OrderStatusChange;
import com.zyozmen.products.domain.model.TrazabilidadPresentacion;
import com.zyozmen.products.domain.port.in.MarketplaceUseCase;
import com.zyozmen.products.domain.port.in.OrderUseCase;
import com.zyozmen.products.domain.port.out.OrderRepositoryPort;
import com.zyozmen.products.domain.port.out.UserRepositoryPort;
import com.zyozmen.products.domain.port.out.InventarioOrdenPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private MarketplaceUseCase marketplace;

    @Mock
    private UserRepositoryPort userRepository;
    @Mock private InventarioOrdenPort inventario;
    @Mock private com.zyozmen.products.domain.port.out.OrderSettlementPort settlements;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createPqrForcesZeroTotalsAndPqrFlag() {
        when(userRepository.existsByUsername("juan")).thenReturn(true);
        when(marketplace.resolverPresentacion("presentacion")).thenReturn(activeProduct());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order created = orderService.createPqr("juan",
                List.of(new OrderUseCase.PqrItemRequest("presentacion", "Nombre no confiable", 1)), null, "motivo");

        assertThat(created.isPqr()).isTrue();
        assertThat(created.getStatus()).isEqualTo(OrderStatus.CREADA);
        assertThat(created.getTotal()).isEqualByComparingTo("0");
        assertThat(created.getSubtotal()).isEqualByComparingTo("0");
        assertThat(created.getTaxAmount()).isEqualByComparingTo("0");
        assertThat(created.getItems().get(0).getUnitPrice()).isEqualByComparingTo("0");
        assertThat(created.getItems().get(0).getName()).isEqualTo("Tangerine");
        assertThat(created.getItems().get(0).getTrazabilidad().costoPresentacion())
                .isEqualByComparingTo("10000");
        assertThat(created.getNotes()).isEqualTo("motivo");
        assertThat(created.isInventoryDeducted()).isTrue();
        verify(inventario).descontar(created.getItems());
        verify(settlements).recognizeOrderMovement(created);
    }

    @Test
    void createPqrRejectsUnknownUser() {
        when(userRepository.existsByUsername("nadie")).thenReturn(false);

        assertThatThrownBy(() -> orderService.createPqr("nadie",
                List.of(new OrderUseCase.PqrItemRequest(null, "X", 1)), null, null))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void normalOrderCannotSkipPaymentToPacked() {
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order("juan", OrderStatus.CREADA)));

        assertThatThrownBy(() -> orderService.changeStatus(101L, OrderStatus.EMPACADA, "admin"))
                .isInstanceOf(OrderConflictException.class);
    }

    @Test
    void pqrOrderGoesFromCreatedToPackedAndCannotBePaid() {
        Order pqr = order("juan", OrderStatus.CREADA);
        pqr.setPqr(true);
        when(orderRepository.findById(101L)).thenReturn(Optional.of(pqr));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> orderService.changeStatus(101L, OrderStatus.PAGADA, "admin"))
                .isInstanceOf(OrderConflictException.class);
        Order packed = orderService.changeStatus(101L, OrderStatus.EMPACADA, "admin");

        assertThat(packed.getStatus()).isEqualTo(OrderStatus.EMPACADA);
        assertThat(packed.getPackedAt()).isNotNull();
    }

    @Test
    void paidStoresCashOnDeliveryFlagAndTimestamp() {
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order("juan", OrderStatus.CREADA)));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order paid = orderService.changeStatus(101L, OrderStatus.PAGADA, "admin",
                new OrderUseCase.StatusChangeDetails(true, null, null, null));

        assertThat(paid.isCashOnDelivery()).isTrue();
        assertThat(paid.getPaidAt()).isNotNull();
        assertThat(paid.isInventoryDeducted()).isTrue();
        verify(inventario).descontar(paid.getItems());
        verifyNoInteractions(settlements);
    }

    @Test
    void deliveredCashOnDeliveryRequiresPaymentDetail() {
        Order packed = order("juan", OrderStatus.EMPACADA);
        packed.setCashOnDelivery(true);
        when(orderRepository.findById(101L)).thenReturn(Optional.of(packed));

        assertThatThrownBy(() -> orderService.changeStatus(101L, OrderStatus.ENTREGADA, "admin"))
                .isInstanceOf(OrderValidationException.class);
    }

    @Test
    void deliveredIgnoresPaymentDetailForOrdersWithoutCashOnDelivery() {
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order("juan", OrderStatus.EMPACADA)));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order delivered = orderService.changeStatus(101L, OrderStatus.ENTREGADA, "admin",
                new OrderUseCase.StatusChangeDetails(null, new BigDecimal("30000"), new BigDecimal("1000"), null));

        assertThat(delivered.getCashAmount()).isNull();
        assertThat(delivered.getCardAmount()).isNull();
        assertThat(delivered.getDeliveredAt()).isNotNull();
        verifyNoInteractions(settlements);
    }

    @Test
    void deliveredAcceptsMatchingPaymentForCashOnDelivery() {
        Order withDetail = order("juan", OrderStatus.EMPACADA);
        withDetail.setCashOnDelivery(true);
        when(orderRepository.findById(101L)).thenReturn(Optional.of(withDetail));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order delivered = orderService.changeStatus(101L, OrderStatus.ENTREGADA, "admin",
                new OrderUseCase.StatusChangeDetails(null, new BigDecimal("30000.00"),
                        new BigDecimal("4800.00"), null));

        assertThat(delivered.getCashAmount()).isEqualByComparingTo("30000");
        assertThat(delivered.getCardAmount()).isEqualByComparingTo("4800");
        assertThat(delivered.getDeliveredAt()).isNotNull();
        verify(settlements).recognizeOrderMovement(delivered);
        verifyNoInteractions(inventario);
    }

    @Test
    void deliveredRejectsPaymentThatDoesNotMatchTotalForCashOnDelivery() {
        Order cod = order("juan", OrderStatus.EMPACADA);
        cod.setCashOnDelivery(true);
        when(orderRepository.findById(101L)).thenReturn(Optional.of(cod));

        assertThatThrownBy(() -> orderService.changeStatus(101L, OrderStatus.ENTREGADA, "admin",
                new OrderUseCase.StatusChangeDetails(null, new BigDecimal("30000"), new BigDecimal("1000"), null)))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("suma");
        verifyNoInteractions(settlements);
    }

    @Test
    void reportWeekCoversMondayToMondayAndExcludesPqrFromPaid() {
        Order delivered = order("juan", OrderStatus.ENTREGADA);
        delivered.setCashAmount(new BigDecimal("20000.00"));
        delivered.setCardAmount(new BigDecimal("10000.00"));
        Order pqr = order("ana", OrderStatus.ENTREGADA);
        pqr.setPqr(true);
        pqr.setTotal(new BigDecimal("0.00"));
        when(orderRepository.findAll(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                delivered, pqr, order("a", OrderStatus.CANCELADA), order("b", OrderStatus.PAGADA),
                order("c", OrderStatus.EMPACADA))));

        // 2026-10-11 es domingo: su semana va del lunes 2026-10-05 al lunes 2026-10-12 (exclusivo).
        OrderUseCase.OrderReport report = orderService.report(LocalDate.of(2026, 10, 11),
                OrderUseCase.ReportPeriod.WEEK);

        assertThat(report.from()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(report.to()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(report.totalOrders()).isEqualTo(5);
        assertThat(report.paid().count()).isEqualTo(1);
        assertThat(report.paid().total()).isEqualByComparingTo("34800");
        assertThat(report.paid().cash()).isEqualByComparingTo("20000");
        assertThat(report.paid().card()).isEqualByComparingTo("10000");
        assertThat(report.paid().undetailed()).isEqualByComparingTo("4800");
        assertThat(report.cancelled().count()).isEqualTo(1);
        assertThat(report.toPack().count()).isEqualTo(1);
        assertThat(report.toDeliver().count()).isEqualTo(1);
        assertThat(report.pqr().count()).isEqualTo(1);

        ArgumentCaptor<Instant> from = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> to = ArgumentCaptor.forClass(Instant.class);
        verify(orderRepository).findAll(any(), from.capture(), to.capture(), any(), any());
        assertThat(from.getValue()).isEqualTo(Instant.parse("2026-10-05T05:00:00Z"));
        assertThat(to.getValue()).isEqualTo(Instant.parse("2026-10-12T05:00:00Z"));
    }

    @Test
    void reportMonthAndDayRanges() {
        when(orderRepository.findAll(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        OrderUseCase.OrderReport month = orderService.report(LocalDate.of(2026, 2, 17), OrderUseCase.ReportPeriod.MONTH);
        OrderUseCase.OrderReport day = orderService.report(LocalDate.of(2026, 2, 17), OrderUseCase.ReportPeriod.DAY);

        assertThat(month.from()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(month.to()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(day.from()).isEqualTo(LocalDate.of(2026, 2, 17));
        assertThat(day.to()).isEqualTo(LocalDate.of(2026, 2, 18));
    }

    @Test
    void createUsesMarketplacePriceAndSnapshotForAuthenticatedUser() {
        when(marketplace.resolverPresentacion("presentacion")).thenReturn(activeProduct());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order created = orderService.create("juan",
                List.of(new OrderUseCase.OrderItemRequest("presentacion", 2,
                        new BigDecimal("15000.00"), new BigDecimal("30000.00"))),
                new OrderUseCase.OrderDeliveryRequest("Juan", "Calle 1", "", "3001234567"),
                new OrderUseCase.OrderAmountsRequest(
                        new BigDecimal("30000.00"), new BigDecimal("4800.00"), new BigDecimal("34800.00")));

        assertThat(created.getUsername()).isEqualTo("juan");
        assertThat(created.getStatus()).isEqualTo(OrderStatus.CREADA);
        assertThat(created.getSubtotal()).isEqualByComparingTo("30000.00");
        assertThat(created.getTaxAmount()).isEqualByComparingTo("4800.00");
        assertThat(created.getTotal()).isEqualByComparingTo("34800.00");
        assertThat(created.getItems()).containsExactly(OrderItem.builder()
                .productId("presentacion").name("Tangerine").quantity(2)
                .unitPrice(new BigDecimal("15000.00")).lineTotal(new BigDecimal("30000.00"))
                .trazabilidad(activeProduct().trazabilidad()).build());
        assertThat(created.getDelivery().getRecipientName()).isEqualTo("Juan");
        assertThat(created.getCreatedAt()).isNotNull();
        verify(orderRepository).save(created);
        verifyNoInteractions(inventario);
    }

    @Test
    void createRejectsStaleClientPrice() {
        when(marketplace.resolverPresentacion("presentacion")).thenReturn(activeProduct());

        assertThatThrownBy(this::createWithStalePrice)
                .isInstanceOf(OrderProductException.class)
                .hasMessageContaining("precio");

        verifyNoInteractions(orderRepository);
    }

    @Test
    void createRejectsMissingOrInactiveProduct() {
        when(marketplace.resolverPresentacion("presentacion"))
                .thenThrow(new ResourceNotFoundException("Presentacion no encontrada o inactiva"));

        assertThatThrownBy(this::createWithMissingProduct)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no encontrada o inactiva");
    }

    @Test
    void findOwnedOrderDoesNotExposeAnotherUsersOrder() {
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order("ana", OrderStatus.CREADA)));

        assertThatThrownBy(() -> orderService.findOwnedOrder(101L, "juan"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changeStatusRecordsAuditEntry() {
        Order order = order("juan", OrderStatus.CREADA);
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updated = orderService.changeStatus(101L, OrderStatus.PAGADA, "admin");

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PAGADA);
        assertThat(updated.getStatusHistory()).containsExactly(OrderStatusChange.builder()
                .fromStatus(OrderStatus.CREADA)
                .toStatus(OrderStatus.PAGADA)
                .changedBy("admin")
                .changedAt(updated.getUpdatedAt())
                .build());
        verify(orderRepository).save(order);
    }

    @Test
    void changeStatusRejectsTransitionFromFinalState() {
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order("juan", OrderStatus.ENTREGADA)));

        assertThatThrownBy(() -> orderService.changeStatus(101L, OrderStatus.CANCELADA, "admin"))
                .isInstanceOf(OrderConflictException.class)
                .hasMessageContaining("No se permite");
    }

    @Test
    void cancelarDevuelveStockUnaSolaVezYLaSegundaSolicitudNoTocaInventario() {
        var order = order("juan", OrderStatus.CREADA);
        order.setPqr(true);
        order.setInventoryDeducted(true);
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var cancelada = orderService.changeStatus(101L, OrderStatus.CANCELADA, "admin");

        assertThat(cancelada.isInventoryDeducted()).isFalse();
        verify(inventario).restituir(order.getItems());
        verify(settlements).recognizeOrderMovement(cancelada);
        assertThatThrownBy(() -> orderService.changeStatus(101L, OrderStatus.CANCELADA, "admin"))
                .isInstanceOf(OrderConflictException.class);
        org.mockito.Mockito.verifyNoMoreInteractions(inventario);
    }

    @Test
    void ventasPagadasNoSeCancelanIncluyendoContraEntregaYEmpacadasHistoricas() {
        for (var status : List.of(OrderStatus.PAGADA, OrderStatus.EMPACADA)) {
            for (boolean cod : List.of(true, false)) {
                var order = order("juan", status);
                order.setCashOnDelivery(cod);
                when(orderRepository.findById(101L)).thenReturn(Optional.of(order));
                assertThatThrownBy(() -> orderService.changeStatus(101L, OrderStatus.CANCELADA, "admin"))
                        .isInstanceOf(OrderConflictException.class);
            }
        }
        verifyNoInteractions(inventario, settlements);
    }

    @Test
    void pagoNormalReconoceCostoUnaVezPeroNoLoHaceAlEmpacarNiEntregar() {
        var order = order("juan", OrderStatus.CREADA);
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        orderService.changeStatus(101L, OrderStatus.PAGADA, "admin");
        verify(settlements).recognizeOrderMovement(order);
        orderService.changeStatus(101L, OrderStatus.EMPACADA, "admin");
        orderService.changeStatus(101L, OrderStatus.ENTREGADA, "admin");
        org.mockito.Mockito.verifyNoMoreInteractions(settlements);
    }

    @Test
    void cancelarCreadaNoRestituyeStockQueNuncaSeDesconto() {
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order("juan", OrderStatus.CREADA)));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        orderService.changeStatus(101L, OrderStatus.CANCELADA, "admin");
        verifyNoInteractions(inventario);
    }

    @Test
    void reintentarPagadaOAvanzarAEmpacadaNoDescuentaDosVeces() {
        var order = order("juan", OrderStatus.CREADA);
        when(orderRepository.findById(101L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        orderService.changeStatus(101L, OrderStatus.PAGADA, "admin");
        assertThatThrownBy(() -> orderService.changeStatus(101L, OrderStatus.PAGADA, "admin"))
                .isInstanceOf(OrderConflictException.class);
        orderService.changeStatus(101L, OrderStatus.EMPACADA, "admin");
        verify(inventario).descontar(order.getItems());
        org.mockito.Mockito.verifyNoMoreInteractions(inventario);
    }

    private MarketplaceUseCase.PresentacionVenta activeProduct() {
        return new MarketplaceUseCase.PresentacionVenta("Tangerine", new BigDecimal("15000.00"),
                new TrazabilidadPresentacion("proveedor", "lote", "contenedor", "cepa", "presentacion",
                        BigDecimal.TEN, "GRAMOS", new BigDecimal("1000"), new BigDecimal("10000")));
    }

    private OrderDelivery deliveryDomain() {
        return OrderDelivery.builder().recipientName("Juan").address("Calle 1")
                .addressComplement("").contactPhone("3001234567").build();
    }

    private OrderUseCase.OrderDeliveryRequest delivery() {
        return new OrderUseCase.OrderDeliveryRequest("Juan", "Calle 1", "", "3001234567");
    }

    private Order createWithStalePrice() {
        return orderService.create("juan",
                List.of(new OrderUseCase.OrderItemRequest("presentacion", 2,
                        new BigDecimal("14000.00"), new BigDecimal("28000.00"))),
                delivery(), new OrderUseCase.OrderAmountsRequest(
                        new BigDecimal("28000.00"), new BigDecimal("4480.00"), new BigDecimal("32480.00")));
    }

    private Order createWithMissingProduct() {
        return orderService.create("juan",
                List.of(new OrderUseCase.OrderItemRequest("presentacion", 1,
                        new BigDecimal("15000.00"), new BigDecimal("15000.00"))),
                delivery(), new OrderUseCase.OrderAmountsRequest(
                        new BigDecimal("15000.00"), new BigDecimal("2400.00"), new BigDecimal("17400.00")));
    }

    private Order order(String username, OrderStatus status) {
        return Order.builder().id(101L).username(username).status(status)
                .subtotal(new BigDecimal("30000.00")).taxAmount(new BigDecimal("4800.00"))
                .total(new BigDecimal("34800.00")).delivery(deliveryDomain())
                .items(List.of()).statusHistory(List.of()).build();
    }
}

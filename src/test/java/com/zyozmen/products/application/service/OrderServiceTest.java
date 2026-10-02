package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.OrderConflictException;
import com.zyozmen.products.domain.exception.OrderProductException;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.Order;
import com.zyozmen.products.domain.model.OrderDelivery;
import com.zyozmen.products.domain.model.OrderItem;
import com.zyozmen.products.domain.model.OrderStatus;
import com.zyozmen.products.domain.model.OrderStatusChange;
import com.zyozmen.products.domain.model.Price;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.port.in.OrderUseCase;
import com.zyozmen.products.domain.port.out.OrderRepositoryPort;
import com.zyozmen.products.domain.port.out.ProductoRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
    private ProductoRepositoryPort productoRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createUsesCatalogPriceAndBuildsOrderForAuthenticatedUser() {
        when(productoRepository.findById(12L)).thenReturn(Optional.of(activeProduct()));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order created = orderService.create("juan",
                List.of(new OrderUseCase.OrderItemRequest("12", 2,
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
                .productId("12").name("Producto real").quantity(2)
                .unitPrice(new BigDecimal("15000.00")).lineTotal(new BigDecimal("30000.00")).build());
        assertThat(created.getDelivery().getRecipientName()).isEqualTo("Juan");
        assertThat(created.getCreatedAt()).isNotNull();
        verify(orderRepository).save(created);
    }

    @Test
    void createRejectsStaleClientPrice() {
        when(productoRepository.findById(12L)).thenReturn(Optional.of(activeProduct()));

        assertThatThrownBy(this::createWithStalePrice)
                .isInstanceOf(OrderProductException.class)
                .hasMessageContaining("precio");

        verifyNoInteractions(orderRepository);
    }

    @Test
    void createRejectsMissingOrInactiveProduct() {
        when(productoRepository.findById(12L)).thenReturn(Optional.empty());

        assertThatThrownBy(this::createWithMissingProduct)
                .isInstanceOf(OrderProductException.class)
                .hasMessageContaining("no existe o no está activo");
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

    private Producto activeProduct() {
        return Producto.builder().id("12").name("Producto real").status("active")
                .price(Price.builder().current(new BigDecimal("15000.00")).build()).build();
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
                List.of(new OrderUseCase.OrderItemRequest("12", 2,
                        new BigDecimal("14000.00"), new BigDecimal("28000.00"))),
                delivery(), new OrderUseCase.OrderAmountsRequest(
                        new BigDecimal("28000.00"), new BigDecimal("4480.00"), new BigDecimal("32480.00")));
    }

    private Order createWithMissingProduct() {
        return orderService.create("juan",
                List.of(new OrderUseCase.OrderItemRequest("12", 1,
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

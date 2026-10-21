package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.OrderCreateRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.OrderResponseDTO;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.Order;
import com.zyozmen.products.domain.model.OrderDelivery;
import com.zyozmen.products.domain.model.OrderStatus;
import com.zyozmen.products.domain.port.in.OrderUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdersControllerTest {

    @Mock
    private OrderUseCase orderUseCase;

    @InjectMocks
    private OrdersController controller;

    private final TestingAuthenticationToken authentication = new TestingAuthenticationToken("juan", null);

    @Test
    void createMapsItemsDeliveryAndAmountsForAuthenticatedUser() {
        var request = new OrderCreateRequestDTO(
                List.of(new OrderCreateRequestDTO.ItemRequest("p1", "Product", 2, BigDecimal.TEN, BigDecimal.valueOf(20))),
                new OrderCreateRequestDTO.DeliveryRequest("Juan", "Street 1", "Apt 2", "123"),
                BigDecimal.valueOf(20), BigDecimal.ONE, BigDecimal.valueOf(21));
        var items = List.of(new OrderUseCase.OrderItemRequest("p1", 2, BigDecimal.TEN, BigDecimal.valueOf(20)));
        var delivery = new OrderUseCase.OrderDeliveryRequest("Juan", "Street 1", "Apt 2", "123");
        var amounts = new OrderUseCase.OrderAmountsRequest(BigDecimal.valueOf(20), BigDecimal.ONE, BigDecimal.valueOf(21));
        Order order = order();
        when(orderUseCase.create("juan", items, delivery, amounts)).thenReturn(order);

        var response = controller.create(authentication, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(OrderResponseDTO.from(order));
    }

    @Test
    void createNormalizesNullDeliveryFieldsToEmptyStrings() {
        var request = new OrderCreateRequestDTO(
                List.of(new OrderCreateRequestDTO.ItemRequest("p1", null, 1, BigDecimal.TEN, BigDecimal.TEN)),
                new OrderCreateRequestDTO.DeliveryRequest(null, null, null, null),
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN);
        var items = List.of(new OrderUseCase.OrderItemRequest("p1", 1, BigDecimal.TEN, BigDecimal.TEN));
        var delivery = new OrderUseCase.OrderDeliveryRequest("", "", "", "");
        var amounts = new OrderUseCase.OrderAmountsRequest(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN);
        when(orderUseCase.create("juan", items, delivery, amounts)).thenReturn(order());

        assertThat(controller.create(authentication, request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void findMineReturnsMappedOrdersForAuthenticatedUser() {
        Order order = order();
        when(orderUseCase.findMine("juan")).thenReturn(List.of(order));

        var response = controller.findMine(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(OrderResponseDTO.from(order));
    }

    @Test
    void findMineReturnsEmptyListWhenUserHasNoOrders() {
        when(orderUseCase.findMine("juan")).thenReturn(List.of());

        assertThat(controller.findMine(authentication).getBody()).isEmpty();
    }

    @Test
    void findOwnedOrderPassesAuthenticatedUsername() {
        Order order = order();
        when(orderUseCase.findOwnedOrder(101L, "juan")).thenReturn(order);

        var response = controller.findOwnedOrder(authentication, 101L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(OrderResponseDTO.from(order));
    }

    @Test
    void findOwnedOrderPropagatesNotFound() {
        when(orderUseCase.findOwnedOrder(101L, "juan")).thenThrow(new ResourceNotFoundException("Order not found"));

        assertThatThrownBy(() -> controller.findOwnedOrder(authentication, 101L))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Order not found");
    }

    private Order order() {
        return Order.builder().id(101L).username("juan").status(OrderStatus.CREADA)
                .items(List.of()).delivery(new OrderDelivery()).build();
    }
}

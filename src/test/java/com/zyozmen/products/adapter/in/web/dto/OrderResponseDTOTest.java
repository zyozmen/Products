package com.zyozmen.products.adapter.in.web.dto;

import com.zyozmen.products.domain.model.Order;
import com.zyozmen.products.domain.model.OrderDelivery;
import com.zyozmen.products.domain.model.OrderStatus;
import com.zyozmen.products.domain.model.OrderItem;
import com.zyozmen.products.domain.model.PresentationTrace;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderResponseDTOTest {
    @Test
    void includesPresentationContentAndUnitForNormalAndPqrOrders() throws Exception {
        var trace = new PresentationTrace("provider", "lot", "container", "strain", "presentation",
                new BigDecimal("40.5"), "GRAMOS", BigDecimal.ONE, BigDecimal.TEN);
        var item = OrderItem.builder().productId("presentation").quantity(2).trazabilidad(trace).build();
        for (boolean pqr : new boolean[]{false, true}) {
            Order order = Order.builder().status(OrderStatus.CREADA).pqr(pqr).items(List.of(item))
                    .delivery(OrderDelivery.builder().build()).build();

            var response = OrderResponseDTO.from(order);
            var json = new ObjectMapper().valueToTree(response).get("items").get(0);

            assertThat(response.items().getFirst().contenidoUnidad()).isEqualByComparingTo("40.5");
            assertThat(response.items().getFirst().unidadBase()).isEqualTo("GRAMOS");
            assertThat(json.get("contenidoUnidad").decimalValue()).isEqualByComparingTo("40.5");
            assertThat(json.get("unidadBase").asText()).isEqualTo("GRAMOS");
            assertThat(json.has("costoBasePorUnidad")).isFalse();
            assertThat(json.has("costoPresentacion")).isFalse();
        }
    }

    @Test
    void historicalOrderWithoutTraceReturnsNullContentAndUnit() {
        Order order = Order.builder().status(OrderStatus.CREADA)
                .items(List.of(OrderItem.builder().productId("legacy").build()))
                .delivery(OrderDelivery.builder().build()).build();

        var item = OrderResponseDTO.from(order).items().getFirst();

        assertThat(item.contenidoUnidad()).isNull();
        assertThat(item.unidadBase()).isNull();
    }

    @Test
    void onlyNonPqrCashOnDeliveryOrdersExposePaymentAmounts() {
        for (boolean pqr : new boolean[]{false, true}) {
            for (boolean cashOnDelivery : new boolean[]{false, true}) {
                Order order = Order.builder().status(OrderStatus.ENTREGADA).pqr(pqr)
                        .cashOnDelivery(cashOnDelivery).cashAmount(BigDecimal.ZERO)
                        .cardAmount(BigDecimal.TEN).items(List.of())
                        .delivery(OrderDelivery.builder().build()).build();

                var response = OrderResponseDTO.from(order);

                if (cashOnDelivery && !pqr) {
                    assertThat(response.cashAmount()).isEqualByComparingTo(BigDecimal.ZERO);
                    assertThat(response.cardAmount()).isEqualByComparingTo(BigDecimal.TEN);
                } else {
                    assertThat(response.cashAmount()).isNull();
                    assertThat(response.cardAmount()).isNull();
                }
            }
        }
    }
}

package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.OrderCreateRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.OrderResponseDTO;
import com.zyozmen.products.adapter.in.web.dto.OrderStatusRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.PqrOrderCreateRequestDTO;
import com.zyozmen.products.domain.model.Order;
import com.zyozmen.products.domain.model.OrderDelivery;
import com.zyozmen.products.domain.model.OrderStatus;
import com.zyozmen.products.domain.port.in.OrderUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrdersControllerTest {

    @Mock
    private OrderUseCase orderUseCase;

    @InjectMocks
    private AdminOrdersController controller;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void findAllWithoutFiltersReturnsUnpagedOrders(String empty) {
        Order order = order();
        when(orderUseCase.findAll(null, null, null, null, null))
                .thenReturn(new PageImpl<>(List.of(order)));

        var response = controller.findAll(empty, empty, empty, null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders()).doesNotContainKey("X-Total-Count");
        assertThat(response.getBody()).containsExactly(OrderResponseDTO.from(order));
    }

    @Test
    void findAllConvertsDatesToBogotaRangeAndReturnsTotalCount() {
        Instant from = Instant.parse("2026-10-01T05:00:00Z");
        Instant to = Instant.parse("2026-10-21T05:00:00Z");
        PageRequest pageable = PageRequest.of(1, 10);
        Order order = order();
        when(orderUseCase.findAll(OrderStatus.CREADA, from, to, "juan", pageable))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 25));

        var response = controller.findAll("CREADA", "2026-10-01", "2026-10-20", "juan", 1, 10);

        assertThat(response.getHeaders().getFirst("X-Total-Count")).isEqualTo("25");
        assertThat(response.getBody()).containsExactly(OrderResponseDTO.from(order));
    }

    @ParameterizedTest
    @CsvSource(value = {"null,5,0,5", "2,null,2,20"}, nullValues = "null")
    void findAllUsesDefaultPageOrSize(Integer page, Integer size, int expectedPage, int expectedSize) {
        PageRequest pageable = PageRequest.of(expectedPage, expectedSize);
        when(orderUseCase.findAll(null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertThat(controller.findAll(null, null, null, null, page, size).getBody()).isEmpty();
        verify(orderUseCase).findAll(null, null, null, null, pageable);
    }

    @ParameterizedTest
    @CsvSource({"-1,20", "0,0", "0,201"})
    void findAllRejectsInvalidPagination(int page, int size) {
        assertThatThrownBy(() -> controller.findAll(null, null, null, null, page, size))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("200");
        verifyNoInteractions(orderUseCase);
    }

    @ParameterizedTest
    @CsvSource({
            "INVALID,2026-10-01,2026-10-20",
            "CREADA,invalid,2026-10-20",
            "CREADA,2026-10-01,invalid",
            "CREADA,2026-10-22,2026-10-20"
    })
    void findAllRejectsInvalidFilters(String status, String from, String to) {
        assertThatThrownBy(() -> controller.findAll(status, from, to, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(orderUseCase);
    }

    @Test
    void changeStatusPassesActorAndPaymentDetails() {
        var request = new OrderStatusRequestDTO("PAGADA", true, BigDecimal.TEN, BigDecimal.ONE, "paid");
        var details = new OrderUseCase.StatusChangeDetails(true, BigDecimal.TEN, BigDecimal.ONE, "paid");
        Order order = order();
        order.setStatus(OrderStatus.PAGADA);
        when(orderUseCase.changeStatus(101L, OrderStatus.PAGADA, "admin", details)).thenReturn(order);

        var response = controller.changeStatus(new TestingAuthenticationToken("admin", null), 101L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(OrderResponseDTO.from(order));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "INVALID"})
    void changeStatusRejectsMissingOrUnknownStatus(String status) {
        var request = new OrderStatusRequestDTO(status, null, null, null, null);

        assertThatThrownBy(() -> controller.changeStatus(
                new TestingAuthenticationToken("admin", null), 101L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Estado de orden inv\u00e1lido");
        verifyNoInteractions(orderUseCase);
    }

    @Test
    void createPqrMapsItemsAndOptionalDeliveryFields() {
        var request = new PqrOrderCreateRequestDTO("juan",
                List.of(new PqrOrderCreateRequestDTO.ItemRequest("p1", "Replacement", 2)),
                new OrderCreateRequestDTO.DeliveryRequest("Juan", null, "Apt 2", null), "damaged");
        var items = List.of(new OrderUseCase.PqrItemRequest("p1", "Replacement", 2));
        var delivery = new OrderUseCase.OrderDeliveryRequest("Juan", "", "Apt 2", "");
        Order order = order();
        order.setPqr(true);
        when(orderUseCase.createPqr("juan", items, delivery, "damaged")).thenReturn(order);

        var response = controller.createPqr(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(OrderResponseDTO.from(order));
    }

    @Test
    void createPqrAllowsMissingDelivery() {
        var request = new PqrOrderCreateRequestDTO("juan",
                List.of(new PqrOrderCreateRequestDTO.ItemRequest(null, "Replacement", 1)), null, null);
        var items = List.of(new OrderUseCase.PqrItemRequest(null, "Replacement", 1));
        when(orderUseCase.createPqr("juan", items, null, null)).thenReturn(order());

        assertThat(controller.createPqr(request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(orderUseCase).createPqr("juan", items, null, null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"day", "week", "month"})
    void reportMapsPeriodAndSummary(String period) {
        LocalDate date = LocalDate.of(2026, 10, 20);
        var summary = new OrderUseCase.OrderReport(date, date.plusDays(1), 8,
                new OrderUseCase.PaidSummary(3, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.valueOf(9)),
                new OrderUseCase.CountSummary(1), new OrderUseCase.CountSummary(2),
                new OrderUseCase.CountSummary(1), new OrderUseCase.CountSummary(1));
        when(orderUseCase.report(date, OrderUseCase.ReportPeriod.valueOf(period.toUpperCase(Locale.ROOT))))
                .thenReturn(summary);

        var response = controller.report("2026-10-20", period);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().from()).isEqualTo(date);
        assertThat(response.getBody().to()).isEqualTo(date.plusDays(1));
        assertThat(response.getBody().totalOrders()).isEqualTo(8);
        assertThat(response.getBody().paid().count()).isEqualTo(3);
        assertThat(response.getBody().paid().total()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(response.getBody().cancelled().count()).isEqualTo(1);
        assertThat(response.getBody().toPack().count()).isEqualTo(2);
        assertThat(response.getBody().toDeliver().count()).isEqualTo(1);
        assertThat(response.getBody().pqr().count()).isEqualTo(1);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void reportDefaultsToTodayInBogota(String date) {
        LocalDate before = LocalDate.now(ZoneId.of("America/Bogota"));
        var summary = new OrderUseCase.OrderReport(before, before.plusDays(1), 0,
                new OrderUseCase.PaidSummary(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                new OrderUseCase.CountSummary(0), new OrderUseCase.CountSummary(0),
                new OrderUseCase.CountSummary(0), new OrderUseCase.CountSummary(0));
        when(orderUseCase.report(any(LocalDate.class), eq(OrderUseCase.ReportPeriod.DAY)))
                .thenReturn(summary);

        controller.report(date, "day");

        var captor = ArgumentCaptor.forClass(LocalDate.class);
        verify(orderUseCase).report(captor.capture(), eq(OrderUseCase.ReportPeriod.DAY));
        assertThat(captor.getValue()).isBetween(before, LocalDate.now(ZoneId.of("America/Bogota")));
    }

    @ParameterizedTest
    @CsvSource({"invalid,day", "2026-10-20,year"})
    void reportRejectsInvalidDateOrPeriod(String date, String period) {
        assertThatThrownBy(() -> controller.report(date, period)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(orderUseCase);
    }

    @Test
    void findByIdReturnsOrder() {
        Order order = order();
        when(orderUseCase.findById(101L)).thenReturn(order);

        var response = controller.findById(101L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(OrderResponseDTO.from(order));
    }

    private Order order() {
        return Order.builder().id(101L).username("juan").status(OrderStatus.CREADA)
                .items(List.of()).delivery(new OrderDelivery()).build();
    }
}

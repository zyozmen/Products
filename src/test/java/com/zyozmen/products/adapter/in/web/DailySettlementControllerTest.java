package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.domain.port.in.DailySettlementUseCase;
import com.zyozmen.products.domain.model.DailySettlementDetail;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailySettlementControllerTest {

    @Test
    void mapsThePaginatedDailySummary() {
        DailySettlementUseCase useCase = mock(DailySettlementUseCase.class);
        DailySettlementController controller = new DailySettlementController(useCase);
        LocalDate date = LocalDate.of(2026, 11, 3);
        var provider = new DailySettlementUseCase.ProviderSummary("provider-1", "Proveedor", date,
                2, 0, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN,
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.valueOf(50),
                BigDecimal.TEN, BigDecimal.ZERO);
        when(useCase.summary(date, "provider-1", 1, 10)).thenReturn(
                new DailySettlementUseCase.DailySummary(date, "America/Bogota", false,
                        Instant.parse("2026-11-03T05:00:00Z"),
                        new PageImpl<>(List.of(provider), PageRequest.of(1, 10), 11)));

        var response = controller.summary(date, "provider-1", 1, 10);

        assertThat(response.date()).isEqualTo(date);
        assertThat(response.timezone()).isEqualTo("America/Bogota");
        assertThat(response.content()).containsExactly(provider);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(11);
        assertThat(response.totalPages()).isEqualTo(2);
        verify(useCase).summary(date, "provider-1", 1, 10);
    }

    @Test
    void mapsThePaginatedDailyDetail() {
        DailySettlementUseCase useCase = mock(DailySettlementUseCase.class);
        DailySettlementController controller = new DailySettlementController(useCase);
        LocalDate date = LocalDate.of(2026, 11, 3);
        var movement = new DailySettlementDetail("movement-1", "SALE", "6500000000000000000000ab",
                42L, 0, "650000000000000000000201", "650000000000000000000301",
                "strain-1", "650000000000000000000401", 2, BigDecimal.TEN, "GRAMOS",
                BigDecimal.ONE, BigDecimal.valueOf(5), BigDecimal.TEN,
                Instant.parse("2026-11-03T15:00:00Z"), date, null, null, null, null, List.of(),
                "Proveedor", "LOT-1", "CONT-1", "Bolsa 5g", "Sativa");
        when(useCase.detail(date, "6500000000000000000000AB", 1, 10))
                .thenReturn(new PageImpl<>(List.of(movement), PageRequest.of(1, 10), 11));

        var response = controller.detail(date, "6500000000000000000000AB", 1, 10);

        assertThat(response.providerId()).isEqualTo("6500000000000000000000ab");
        assertThat(response.date()).isEqualTo(date);
        assertThat(response.content()).containsExactly(movement);
        assertThat(response.content().getFirst().productName()).isEqualTo("Sativa");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(11);
        assertThat(response.totalPages()).isEqualTo(2);
        verify(useCase).detail(date, "6500000000000000000000AB", 1, 10);
    }
}

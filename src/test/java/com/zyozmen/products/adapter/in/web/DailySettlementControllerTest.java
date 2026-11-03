package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.domain.port.in.DailySettlementUseCase;
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
}

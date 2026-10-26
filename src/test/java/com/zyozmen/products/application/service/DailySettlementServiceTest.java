package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.model.DailySettlementAccumulator;
import com.zyozmen.products.domain.port.out.DailySettlementRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DailySettlementServiceTest {
    private static final String PROVIDER = "6500000000000000000000ab";
    private final DailySettlementRepositoryPort repository = mock(DailySettlementRepositoryPort.class);
    private final DailySettlementService service = new DailySettlementService(repository,
            Clock.fixed(Instant.parse("2026-10-24T04:59:59Z"), ZoneId.of("Asia/Tokyo")));

    @Test
    void fechaPorDefectoUsaBogotaIndependientementeDelRelojDelServidor() {
        when(repository.activationAt()).thenReturn(Optional.empty());
        when(repository.findByDate(any(), any(), any())).thenReturn(Page.empty());
        var result = service.summary(null, null, 0, 20);
        assertThat(result.date()).isEqualTo(LocalDate.of(2026, 10, 23));
        assertThat(result.timezone()).isEqualTo("America/Bogota");
        assertThat(result.activationPending()).isTrue();
        assertThat(result.activatedAt()).isNull();
        verify(repository).findByDate(eq(result.date()), isNull(), argThat(page ->
                page.getPageNumber() == 0 && page.getPageSize() == 20
                        && page.getSort().getOrderFor("providerId").isAscending()));
    }

    @Test
    void medianocheDeBogotaIniciaOtraJornada() {
        when(repository.activationAt()).thenReturn(Optional.empty());
        when(repository.findByDate(any(), any(), any())).thenReturn(Page.empty());
        var midnight = new DailySettlementService(repository,
                Clock.fixed(Instant.parse("2026-10-24T05:00:00Z"), ZoneId.of("UTC")));
        assertThat(midnight.summary(null, null, 0, 20).date()).isEqualTo(LocalDate.of(2026, 10, 24));
    }

    @Test
    void conservaActivacionYNormalizaObjectIdSinCambiarFechaElegida() {
        var activation = Instant.parse("2026-10-23T05:00:00Z");
        when(repository.activationAt()).thenReturn(Optional.of(activation));
        when(repository.findByDate(any(), any(), any())).thenReturn(Page.empty());
        var date = LocalDate.of(2026, 1, 2);
        var result = service.summary(date, PROVIDER.toUpperCase(), 3, 7);
        assertThat(result.date()).isEqualTo(date);
        assertThat(result.activationPending()).isFalse();
        assertThat(result.activatedAt()).isEqualTo(activation);
        verify(repository).findByDate(eq(date), eq(PROVIDER), argThat(p ->
                p.getPageNumber() == 3 && p.getPageSize() == 7));
    }

    @Test
    void brutoNoSeLimitaPorSaldoYSugeridoDescuentaPagosYReversiones() {
        var result = summarize("10000", "4000", "3000", "5000");
        assertThat(result.grossAccumulatedAmount()).isEqualByComparingTo("14000");
        assertThat(result.accumulatedAmount()).isEqualByComparingTo("11000");
        assertThat(result.pendingDailyAmount()).isEqualByComparingTo("6000");
        assertThat(result.suggestedPayableAmount()).isEqualByComparingTo("6000");
        assertThat(result.overpaidAmount()).isEqualByComparingTo("0");
        assertThat(result.totalSalesCount()).isEqualTo(10);
        assertThat(result.totalPqrCount()).isEqualTo(2);
    }

    @Test
    void saldoCeroNoOcultaBrutoYExcesoNoSeConvierteEnDeudaNegativa() {
        var result = summarize("0", "20000", "10000", "25000");
        assertThat(result.accumulatedAmount()).isEqualByComparingTo("20000");
        assertThat(result.pendingDailyAmount()).isEqualByComparingTo("0");
        assertThat(result.suggestedPayableAmount()).isEqualByComparingTo("0");
        assertThat(result.overpaidAmount()).isEqualByComparingTo("5000");
    }

    @Test
    void sugeridoSeLimitaAlSaldoGlobal() {
        var result = summarize("1.25", "0", "0", "0");
        assertThat(result.grossAccumulatedAmount()).isEqualByComparingTo("10000");
        assertThat(result.suggestedPayableAmount()).isEqualByComparingTo("1.25");
        assertThat(result.suggestedPayableAmount().scale()).isEqualTo(2);
    }

    @Test
    void rechazaParametrosInvalidosAntesDeConsultar() {
        for (String provider : List.of("", " ", "123", "zz0000000000000000000000")) {
            assertThatThrownBy(() -> service.summary(null, provider, 0, 20))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> service.summary(null, null, -1, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.summary(null, null, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.summary(null, null, 0, 101)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void erroresDePersistenciaNoSeConviertenEnResumenVacio() {
        when(repository.activationAt()).thenThrow(new IllegalStateException("configuracion ausente"));
        assertThatThrownBy(() -> service.summary(null, null, 0, 20))
                .isInstanceOf(IllegalStateException.class).hasMessage("configuracion ausente");
        verify(repository, never()).findByDate(any(), any(), any());
    }

    @Test
    void noOcultaSaldoAusenteNegativoNiPrecisionMonetariaInvalida() {
        when(repository.activationAt()).thenReturn(Optional.empty());
        var a = accumulator("10000", "0", "0");
        for (var source : List.of(
                new DailySettlementRepositoryPort.ProviderAccumulator(a, null, BigDecimal.ONE),
                new DailySettlementRepositoryPort.ProviderAccumulator(a, "Proveedor", null),
                new DailySettlementRepositoryPort.ProviderAccumulator(a, "Proveedor", new BigDecimal("-1")))) {
            when(repository.findByDate(any(), any(), any())).thenReturn(new PageImpl<>(List.of(source)));
            assertThatThrownBy(() -> service.summary(null, null, 0, 20)).isInstanceOf(IllegalStateException.class);
        }
        when(repository.findByDate(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                new DailySettlementRepositoryPort.ProviderAccumulator(a, "Proveedor", new BigDecimal("1.001")))));
        assertThatThrownBy(() -> service.summary(null, null, 0, 20)).isInstanceOf(ArithmeticException.class);
    }

    @Test
    void acumuladosInconsistentesNoProducenResultadosAparentementeValidos() {
        assertThatThrownBy(() -> accumulator("-1", "0", "0")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> accumulator("10", "11", "0")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new DailySettlementAccumulator(PROVIDER, LocalDate.now(),
                -1, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(IllegalStateException.class);
    }

    private com.zyozmen.products.domain.port.in.DailySettlementUseCase.ProviderSummary summarize(
            String balance, String pqr, String reversed, String paid) {
        when(repository.activationAt()).thenReturn(Optional.empty());
        var a = accumulator(pqr, reversed, paid);
        when(repository.findByDate(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                new DailySettlementRepositoryPort.ProviderAccumulator(a, "Proveedor", new BigDecimal(balance)))));
        return service.summary(null, null, 0, 20).providers().getContent().getFirst();
    }

    private DailySettlementAccumulator accumulator(String pqr, String reversed, String paid) {
        return new DailySettlementAccumulator(PROVIDER, LocalDate.of(2026, 10, 23), 10, 2,
                new BigDecimal("10000"), new BigDecimal(pqr), new BigDecimal(reversed), new BigDecimal(paid));
    }
}

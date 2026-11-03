package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.EventoLiquidacionRequestDTO;
import com.zyozmen.products.domain.port.in.LiquidacionUseCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LiquidacionControllerTest {

    @Test
    void delegatesEventEvaluationAndMapsResponse() {
        LiquidacionUseCase useCase = mock(LiquidacionUseCase.class);
        LiquidacionController controller = new LiquidacionController(useCase);
        BigDecimal amount = new BigDecimal("125.50");
        when(useCase.evaluarEvento(argThat(command ->
                command.proveedorId().equals("provider-1")
                        && command.loteId().equals("lot-1")
                        && command.montoLiquidable().equals(amount))))
                .thenReturn(new LiquidacionUseCase.EvaluacionLiquidacion(true, true, amount, "OK"));

        var response = controller.procesarEvento(
                new EventoLiquidacionRequestDTO("provider-1", "lot-1", amount));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().acumulaLiquidacion()).isTrue();
        assertThat(response.getBody().descontarStock()).isTrue();
        assertThat(response.getBody().montoLiquidable()).isEqualByComparingTo(amount);
        assertThat(response.getBody().motivo()).isEqualTo("OK");
        verify(useCase).evaluarEvento(argThat(command -> command.montoLiquidable().equals(amount)));
    }
}

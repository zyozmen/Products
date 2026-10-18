package com.zyozmen.products.adapter.in.web.dto;

import com.zyozmen.products.domain.port.in.LiquidacionUseCase.EvaluacionLiquidacion;

import java.math.BigDecimal;

public record EvaluacionLiquidacionResponseDTO(boolean acumulaLiquidacion, boolean descontarStock,
                                               BigDecimal montoLiquidable, String motivo) {

    public static EvaluacionLiquidacionResponseDTO from(EvaluacionLiquidacion e) {
        return new EvaluacionLiquidacionResponseDTO(e.acumulaLiquidacion(), e.descontarStock(),
                e.montoLiquidable(), e.motivo());
    }
}

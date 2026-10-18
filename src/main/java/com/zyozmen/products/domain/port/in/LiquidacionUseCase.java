package com.zyozmen.products.domain.port.in;

import java.math.BigDecimal;

public interface LiquidacionUseCase {

    /**
     * Evalúa si un evento de venta/PQR acumula a la liquidación diaria del proveedor.
     * No modifica saldos ni inventario: el descuento de stock siempre procede.
     */
    EvaluacionLiquidacion evaluarEvento(EventoLiquidacionCommand command);

    record EventoLiquidacionCommand(String proveedorId, String loteId, BigDecimal montoLiquidable) {
    }

    record EvaluacionLiquidacion(boolean acumulaLiquidacion, boolean descontarStock, BigDecimal montoLiquidable,
                                 String motivo) {
    }
}

package com.zyozmen.products.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zyozmen.products.domain.port.in.ProveedorUseCase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PagoProveedorResponseDTO(
        ProveedorResponseDTO proveedor,
        String correlationId,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fechaLiquidacion,
        BigDecimal monto,
        List<Asignacion> asignaciones) {

    public record Asignacion(String loteId, BigDecimal monto) {
    }

    public static PagoProveedorResponseDTO from(ProveedorUseCase.ResultadoPago resultado) {
        return new PagoProveedorResponseDTO(ProveedorResponseDTO.from(resultado.proveedor()),
                resultado.correlationId(), resultado.fechaLiquidacion(), resultado.monto(),
                resultado.asignaciones().stream().map(a -> new Asignacion(a.loteId(), a.monto())).toList());
    }
}

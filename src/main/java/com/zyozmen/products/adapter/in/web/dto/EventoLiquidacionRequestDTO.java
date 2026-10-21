package com.zyozmen.products.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record EventoLiquidacionRequestDTO(
        @NotBlank(message = "El proveedorId es obligatorio") String proveedorId,
        @NotBlank(message = "El loteId es obligatorio") String loteId,
        @NotNull(message = "El monto liquidable es obligatorio")
        @DecimalMin(value = "0.00", message = "El monto liquidable no puede ser negativo")
        @Digits(integer = 15, fraction = 2, message = "El monto liquidable admite máximo 2 decimales")
        BigDecimal montoLiquidable) {
}

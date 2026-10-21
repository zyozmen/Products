package com.zyozmen.products.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record LoteCreateRequestDTO(
        @NotBlank(message = "El proveedorId es obligatorio") String proveedorId,
        @NotBlank(message = "El código del lote es obligatorio") String codigoLote,
        @NotEmpty(message = "El lote debe tener al menos una presentación")
        List<@Valid @NotNull(message = "La presentación es obligatoria") PresentacionRequestDTO> presentaciones,
        @DecimalMin(value = "0.00", message = "El abono inicial no puede ser negativo")
        @Digits(integer = 15, fraction = 2, message = "El abono inicial admite máximo 2 decimales")
        BigDecimal abonoInicial) {

    public record PresentacionRequestDTO(
            @NotBlank(message = "El presentacionId es obligatorio") String presentacionId,
            @Min(value = 1, message = "La cantidad de unidades debe ser mayor que cero") int cantidadUnidades,
            @NotNull(message = "El costo unitario es obligatorio")
            @DecimalMin(value = "0.01", message = "El costo unitario debe ser mayor que cero")
            @Digits(integer = 15, fraction = 2, message = "El costo unitario admite máximo 2 decimales")
            BigDecimal costoUnitario) {
    }
}

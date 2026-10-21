package com.zyozmen.products.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record LoteCreateRequestDTO(
        @NotBlank(message = "El proveedorId es obligatorio") String proveedorId,
        @NotEmpty(message = "Debe agregar al menos un contenedor con su capacidad en unidades base para registrar el lote")
        List<@Valid @NotNull(message = "El contenedor es obligatorio") ContenedorRequestDTO> contenedores,
        @DecimalMin(value = "0.00", message = "El abono inicial no puede ser negativo")
        @Digits(integer = 15, fraction = 2, message = "El abono inicial admite máximo 2 decimales")
        BigDecimal abonoInicial) {

    public record ContenedorRequestDTO(
            @NotBlank(message = "El tipo o cepa es obligatorio") String tipoCepa,
            @NotBlank(message = "La unidad base es obligatoria") String unidadBase,
            String presentacionId,
            @NotNull(message = "La capacidad en unidades base es obligatoria")
            @DecimalMin(value = "0", inclusive = false, message = "La capacidad debe ser mayor que cero")
            @Digits(integer = 15, fraction = 3, message = "La capacidad admite máximo 3 decimales")
            BigDecimal capacidadUnidadesBase,
            @NotNull(message = "El costo base por unidad es obligatorio")
            @DecimalMin(value = "0.01", message = "El costo base por unidad debe ser mayor que cero")
            @Digits(integer = 15, fraction = 2, message = "El costo base por unidad admite máximo 2 decimales")
            BigDecimal costoBasePorUnidad) {
    }
}

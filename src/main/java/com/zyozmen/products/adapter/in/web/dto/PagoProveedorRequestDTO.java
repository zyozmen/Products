package com.zyozmen.products.adapter.in.web.dto;

import com.zyozmen.products.domain.model.ProviderTransactionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagoProveedorRequestDTO(
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
        @Digits(integer = 15, fraction = 2, message = "El monto admite máximo 2 decimales")
        BigDecimal monto,
        String loteId,
        ProviderTransactionType tipo,
        @Size(max = 500, message = "Las observaciones son demasiado largas") String observaciones,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fechaLiquidacion) {
}

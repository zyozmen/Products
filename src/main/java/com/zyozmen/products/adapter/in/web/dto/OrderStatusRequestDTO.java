package com.zyozmen.products.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record OrderStatusRequestDTO(
        @NotBlank(message = "El estado es obligatorio") String status,
        Boolean cashOnDelivery,
        @DecimalMin(value = "0.0", message = "El efectivo no puede ser negativo") BigDecimal cashAmount,
        @DecimalMin(value = "0.0", message = "La tarjeta no puede ser negativa") BigDecimal cardAmount,
        String reason) { }

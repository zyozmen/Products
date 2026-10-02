package com.zyozmen.products.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreateRequestDTO(
        @NotEmpty(message = "La orden debe contener al menos un producto")
        List<@NotNull @Valid ItemRequest> items,
        @NotNull(message = "Los datos de entrega son obligatorios") @Valid DeliveryRequest delivery,
        @NotNull @DecimalMin(value = "0.0", message = "El subtotal no puede ser negativo") BigDecimal subtotal,
        @NotNull @DecimalMin(value = "0.0", message = "El impuesto no puede ser negativo") BigDecimal taxAmount,
        @NotNull @DecimalMin(value = "0.0", message = "El total no puede ser negativo") BigDecimal total) {

    public record ItemRequest(
            @NotBlank(message = "El ID del producto es obligatorio") String productId,
            String name,
            @NotNull @Positive(message = "La cantidad debe ser mayor que cero") Integer quantity,
            @NotNull @DecimalMin(value = "0.0", message = "El precio no puede ser negativo") BigDecimal unitPrice,
            @NotNull @DecimalMin(value = "0.0", message = "El total de línea no puede ser negativo") BigDecimal lineTotal) { }

    public record DeliveryRequest(
            String recipientName,
            String address,
            String addressComplement,
            String contactPhone) { }
}

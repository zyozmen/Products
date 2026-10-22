package com.zyozmen.products.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Solicitud para crear una orden PQR. Los importes, el estado y la marca pqr
 * no se leen: el servidor siempre los fuerza (total 0, CREADA, pqr=true).
 */
public record PqrOrderCreateRequestDTO(
        @NotBlank(message = "El username es obligatorio") String username,
        @NotEmpty(message = "La orden debe contener al menos un producto")
        List<@NotNull @Valid ItemRequest> items,
        OrderCreateRequestDTO.DeliveryRequest delivery,
        String notes) {

    public record ItemRequest(
            @NotBlank(message = "El ID de la presentacion es obligatorio") String productId,
            String name,
            @NotNull @Positive(message = "La cantidad debe ser mayor que cero") Integer quantity) { }
}

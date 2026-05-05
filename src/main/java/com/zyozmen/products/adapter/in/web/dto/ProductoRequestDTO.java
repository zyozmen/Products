package com.zyozmen.products.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos de entrada para crear o actualizar un producto")
public class ProductoRequestDTO {

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Schema(description = "Nombre del producto", example = "Laptop Dell XPS 15")
    private String nombre;

    @Schema(description = "Descripción detallada del producto", example = "Procesador Intel Core i7, 16GB RAM")
    private String descripcion;

    @NotNull(message = "El precio del producto es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
    @Schema(description = "Precio del producto", example = "1299.99")
    private BigDecimal precio;
}

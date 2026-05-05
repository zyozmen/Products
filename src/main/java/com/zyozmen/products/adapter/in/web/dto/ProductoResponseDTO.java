package com.zyozmen.products.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos del producto devueltos por la API")
public class ProductoResponseDTO {

    @Schema(description = "Identificador único del producto", example = "1")
    private Long id;

    @Schema(description = "Nombre del producto", example = "Laptop Dell XPS 15")
    private String nombre;

    @Schema(description = "Descripción detallada del producto", example = "Procesador Intel Core i7, 16GB RAM")
    private String descripcion;

    @Schema(description = "Precio del producto", example = "1299.99")
    private BigDecimal precio;
}

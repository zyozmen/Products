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
@Schema(description = "Reseña de un producto")
public class ReviewDTO {

    @Schema(description = "Nombre del autor de la reseña", example = "Jhon Doe")
    private String autor;

    @Schema(description = "Puntuación de 0 a 5 con decimales", example = "3.5")
    private BigDecimal stars;

    @Schema(description = "Texto de la reseña", example = "Excelente producto, muy recomendado.")
    private String review;

    @Schema(description = "Correo del autor", example = "jhon.doe@gmail.com")
    private String email;
}

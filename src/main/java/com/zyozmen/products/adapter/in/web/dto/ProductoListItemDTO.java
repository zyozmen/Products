package com.zyozmen.products.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos resumidos del producto para listados")
public class ProductoListItemDTO {

    @Schema(description = "Identificador único del producto", example = "1")
    private String id;

    @Schema(description = "Nombre del producto", example = "Wireless Noise-Canceling Headphones")
    private String name;

    @JsonProperty("category_ids")
    @Schema(description = "IDs de categorías del producto")
    private List<Long> categoryIds;

    @JsonProperty("current_price")
    @Schema(description = "Precio actual del producto", example = "199.99")
    private BigDecimal currentPrice;

    @JsonProperty("original_price")
    @Schema(description = "Precio original del producto", example = "249.99")
    private BigDecimal originalPrice;

    @JsonProperty("price_currency")
    @Schema(description = "Moneda del precio del producto", example = "USD")
    private String priceCurrency;

    @JsonProperty("average_rating")
    @Schema(description = "Promedio de calificaciones", example = "4.7")
    private BigDecimal averageRating;

    @JsonProperty("total_reviews")
    @Schema(description = "Total de reseñas", example = "128")
    private Integer totalReviews;
}

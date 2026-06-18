package com.zyozmen.products.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
@Schema(description = "Información de ranking del producto")
public class RankingDTO {

    @JsonProperty("average_rating")
    @Schema(description = "Promedio de calificación", example = "4.7")
    private BigDecimal averageRating;

    @JsonProperty("total_reviews")
    @Schema(description = "Total de reseñas", example = "1250")
    private Integer totalReviews;

    @JsonProperty("rating_distribution")
    @Schema(description = "Distribución de calificaciones")
    private RatingDistributionDTO ratingDistribution;
}

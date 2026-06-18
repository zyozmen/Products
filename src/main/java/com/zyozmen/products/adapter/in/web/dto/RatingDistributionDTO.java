package com.zyozmen.products.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Distribución de estrellas")
public class RatingDistributionDTO {

    @JsonProperty("5_star")
    @Schema(description = "Cantidad de reseñas de 5 estrellas", example = "900")
    private Integer fiveStar;

    @JsonProperty("4_star")
    @Schema(description = "Cantidad de reseñas de 4 estrellas", example = "250")
    private Integer fourStar;

    @JsonProperty("3_star")
    @Schema(description = "Cantidad de reseñas de 3 estrellas", example = "70")
    private Integer threeStar;

    @JsonProperty("2_star")
    @Schema(description = "Cantidad de reseñas de 2 estrellas", example = "20")
    private Integer twoStar;

    @JsonProperty("1_star")
    @Schema(description = "Cantidad de reseñas de 1 estrella", example = "10")
    private Integer oneStar;
}

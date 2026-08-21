package com.zyozmen.products.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos de entrada para crear o actualizar un producto")
public class ProductoRequestDTO {

    @Schema(description = "Identificador único del producto (ObjectId)", example = "1")
    private String id;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Schema(description = "Nombre del producto", example = "Wireless Noise-Canceling Headphones")
    private String name;

    @NotBlank(message = "El slug del producto es obligatorio")
    @Schema(description = "Slug único del producto", example = "wireless-noise-canceling-headphones-v2")
    private String slug;

    @Schema(description = "Descripción detallada del producto", example = "High-fidelity audio with advanced active noise cancellation and 40-hour battery life.")
    private String description;

    @NotBlank(message = "El SKU del producto es obligatorio")
    @Schema(description = "SKU del producto", example = "HEAD-WRLS-001")
    private String sku;

    @NotBlank(message = "El estado del producto es obligatorio")
    @Schema(description = "Estado del producto", example = "active")
    private String status;

    @NotEmpty(message = "El producto debe tener al menos una categoría")
    @Schema(description = "Listado de categorías asociadas; debe contener al menos una categoría")
    private List<CategoryDTO> categories;

    @NotNull(message = "La estructura de precio del producto es obligatoria")
    @Schema(description = "Información de precio")
    private PriceDTO price;

    @Schema(description = "Métricas de ranking y reseñas")
    private RankingDTO ranking;

    @JsonProperty("recent_comments")
    @Schema(description = "Comentarios más recientes del producto")
    private List<RecentCommentDTO> recentComments;

    @JsonProperty("has_more_comments")
    @Schema(description = "Indica si hay más comentarios disponibles", example = "true")
    private Boolean hasMoreComments;
}

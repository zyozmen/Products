package com.zyozmen.products.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos del producto devueltos por la API")
public class ProductoResponseDTO {

	@Schema(description = "Identificador único del producto", example = "1")
	private String id;

	@Schema(description = "Nombre del producto", example = "Wireless Noise-Canceling Headphones")
	private String name;

	@Schema(description = "Slug único del producto", example = "wireless-noise-canceling-headphones-v2")
	private String slug;

	@Schema(description = "Descripción detallada del producto", example = "High-fidelity audio with advanced active noise cancellation and 40-hour battery life.")
	private String description;

	@Schema(description = "SKU del producto", example = "HEAD-WRLS-001")
	private String sku;

	@Schema(description = "Estado del producto", example = "active")
	private String status;

	@Schema(description = "Listado de categorías asociadas")
	private List<CategoryDTO> categories;

	@Schema(description = "Información de precio")
	private PriceDTO price;

	@Schema(description = "Métricas de ranking y reseñas")
	private RankingDTO ranking;

	@JsonProperty("recent_comments")
	@Schema(description = "Comentarios más recientes del producto")
	private List<RecentCommentDTO> recentComments;

	@JsonProperty("created_at")
	@Schema(description = "Fecha de creación del producto", example = "2025-11-01T00:00:00Z")
	private Instant createdAt;

	@JsonProperty("updated_at")
	@Schema(description = "Fecha de última actualización del producto", example = "2026-06-17T13:00:00Z")
	private Instant updatedAt;
}

package com.zyozmen.products.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Comentario reciente de un usuario")
public class RecentCommentDTO {

    @JsonProperty("comment_id")
    @Schema(description = "Identificador del comentario", example = "66705c8a1f4b2c3a4f8e90c1")
    private String commentId;

    @JsonProperty("user_id")
    @Schema(description = "Identificador del usuario", example = "66705c8a1f4b2c3a4f8e90d0")
    private String userId;

    @Schema(description = "Nombre de usuario", example = "JohnDoe92")
    private String username;

    @Schema(description = "Calificación entregada", example = "5")
    private Integer rating;

    @Schema(description = "Título del comentario", example = "Amazing sound quality!")
    private String title;

    @Schema(description = "Contenido del comentario", example = "The battery lasts forever and the ANC is top-notch. Highly recommended.")
    private String body;

    @JsonProperty("created_at")
    @Schema(description = "Fecha de creación del comentario", example = "2026-06-15T14:30:00Z")
    private Instant createdAt;
}

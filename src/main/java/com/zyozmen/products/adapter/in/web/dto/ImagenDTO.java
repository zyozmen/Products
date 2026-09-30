package com.zyozmen.products.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Representación DTO de una imagen codificada en Base64")
public class ImagenDTO {

    @Schema(description = "Nombre original del archivo de imagen", example = "producto-principal.jpg")
    private String nombre;

    @Schema(description = "Extensión del archivo de imagen", example = "jpg")
    private String extension;

    @Schema(description = "Contenido en Base64 de la imagen", example = "data:image/jpeg;base64,...")
    private String filepart;
}

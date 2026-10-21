package com.zyozmen.products.adapter.in.web.dto;

import java.util.List;
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
@Schema(description = "Contenedor de imágenes de un producto")
public class ImagesDTO {

    @JsonProperty("foto_principal")
    @Schema(description = "Imagen principal del producto")
    private ImagenDTO fotoPrincipal;

    @JsonProperty("fotos_secundarias")
    @Schema(description = "Listado de hasta 5 imágenes secundarias")
    private List<ImagenDTO> fotosSecundarias;
}

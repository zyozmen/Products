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
@Schema(description = "Categoría asociada al producto")
public class CategoryDTO {

    @JsonProperty("category_id")
    @Schema(description = "Identificador de categoría", example = "1")
    private String categoryId;

    @Schema(description = "Nombre de la categoría", example = "Electronics")
    private String name;

    @Schema(description = "Slug de la categoría", example = "electronics")
    private String slug;
}

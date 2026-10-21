package com.zyozmen.products.adapter.out.mongodb.document;

import org.springframework.data.mongodb.core.mapping.Field;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagenDocument {

    @Field("nombre")
    private String nombre;

    @Field("extension")
    private String extension;

    @Field("filepart")
    private String filepart;
}

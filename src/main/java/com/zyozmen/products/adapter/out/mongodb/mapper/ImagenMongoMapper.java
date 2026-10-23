package com.zyozmen.products.adapter.out.mongodb.mapper;

import com.zyozmen.products.adapter.out.mongodb.document.ImagenDocument;
import com.zyozmen.products.domain.model.Imagen;

public final class ImagenMongoMapper {
    private ImagenMongoMapper() {
    }

    public static ImagenDocument toDocument(Imagen imagen) {
        return imagen == null ? null : new ImagenDocument(
                imagen.getNombre(), imagen.getExtension(), imagen.getFilepart());
    }

    public static Imagen toDomain(ImagenDocument imagen) {
        return imagen == null ? null : new Imagen(
                imagen.getNombre(), imagen.getExtension(), imagen.getFilepart());
    }
}

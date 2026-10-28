package com.zyozmen.products.adapter.out.mongodb.mapper;

import com.zyozmen.products.adapter.out.mongodb.document.ImageDocument;
import com.zyozmen.products.domain.model.Image;

public final class ImageMongoMapper {
    private ImageMongoMapper() {
    }

    public static ImageDocument toDocument(Image imagen) {
        return imagen == null ? null : new ImageDocument(
                imagen.getNombre(), imagen.getExtension(), imagen.getFilepart());
    }

    public static Image toDomain(ImageDocument imagen) {
        return imagen == null ? null : new Image(
                imagen.getNombre(), imagen.getExtension(), imagen.getFilepart());
    }
}

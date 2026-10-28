package com.zyozmen.products.adapter.out.mongodb.mapper;

import com.zyozmen.products.adapter.out.mongodb.document.CatalogCategoryDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ImageDocument;
import com.zyozmen.products.domain.model.CatalogCategory;
import com.zyozmen.products.domain.model.Image;
import org.springframework.stereotype.Component;

@Component
public class CategoryMongoMapper {
    public CatalogCategory toDomain(CatalogCategoryDocument document) {
        ImageDocument image = document.getImage();
        return CatalogCategory.builder().categoryId(document.getCategoryId())
                .name(document.getName()).description(document.getDescription()).imageUrl(document.getImageUrl())
                .createdAt(document.getCreatedAt()).updatedAt(document.getUpdatedAt())
                .image(image == null ? null : new Image(image.getNombre(), image.getExtension(), image.getFilepart()))
                .build();
    }

    public CatalogCategoryDocument toDocument(CatalogCategory category) {
        Image image = category.getImage();
        return CatalogCategoryDocument.builder().categoryId(category.getCategoryId())
                .name(category.getName()).description(category.getDescription()).imageUrl(category.getImageUrl())
                .createdAt(category.getCreatedAt()).updatedAt(category.getUpdatedAt())
                .image(image == null ? null : ImageDocument.builder().nombre(image.getNombre())
                        .extension(image.getExtension()).filepart(image.getFilepart()).build())
                .build();
    }
}

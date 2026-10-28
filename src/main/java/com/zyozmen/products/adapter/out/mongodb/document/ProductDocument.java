package com.zyozmen.products.adapter.out.mongodb.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document("Products")
public record ProductDocument(@Id String id, String nombre, String nombreNormalizado,
                           ImageDocument fotografia, Instant createdAt, Instant updatedAt,
                           @Version Long version, List<Long> categoryIds) {
    public ProductDocument {
        categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
    }

    public ProductDocument(String id, String nombre, String nombreNormalizado, ImageDocument fotografia,
                        Instant createdAt, Instant updatedAt, Long version) {
        this(id, nombre, nombreNormalizado, fotografia, createdAt, updatedAt, version, List.of());
    }
}

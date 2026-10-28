package com.zyozmen.products.domain.model;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.time.Instant;
import java.util.List;

public record Product(String id, String nombre, Image fotografia, Instant createdAt, Instant updatedAt, Long version,
                   List<Long> categoryIds) {
    public Product {
        categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
    }

    public Product(String id, String nombre, Image fotografia, Instant createdAt, Instant updatedAt, Long version) {
        this(id, nombre, fotografia, createdAt, updatedAt, version, List.of());
    }

    public Product(String id, String nombre) {
        this(id, nombre, null, null, null, null);
    }

    public static String normalizar(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.strip().length() > 100) {
            throw new IllegalArgumentException("El nombre de la cepa es obligatorio y admite hasta 100 caracteres");
        }
        return nombre.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    // Identidad anterior, conservada para reconciliar datos de T1 sin cambiar sus referencias.
    public static Product desdeNombre(String nombre) {
        String normalizado = normalizar(nombre);
        String id = UUID.nameUUIDFromBytes(("cepa:" + normalizado).getBytes(StandardCharsets.UTF_8)).toString();
        return new Product(id, normalizado);
    }
}

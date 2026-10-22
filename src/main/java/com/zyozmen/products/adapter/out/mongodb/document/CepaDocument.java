package com.zyozmen.products.adapter.out.mongodb.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("cepas")
public record CepaDocument(@Id String id, String nombre, String nombreNormalizado,
                           ImagenDocument fotografia, Instant createdAt, Instant updatedAt,
                           @Version Long version) { }

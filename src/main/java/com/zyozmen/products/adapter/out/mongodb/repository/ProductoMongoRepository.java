package com.zyozmen.products.adapter.out.mongodb.repository;

import com.zyozmen.products.adapter.out.mongodb.document.ProductoMongoDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Repositorio Spring Data MongoDB.
 * El dominio no lo conoce en absoluto; es un detalle de infraestructura.
 */
public interface ProductoMongoRepository extends MongoRepository<ProductoMongoDocument, String> {

    Optional<ProductoMongoDocument> findBySequenceId(Long sequenceId);

    boolean existsBySequenceId(Long sequenceId);

    void deleteBySequenceId(Long sequenceId);
}

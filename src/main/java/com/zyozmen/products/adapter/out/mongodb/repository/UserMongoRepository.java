package com.zyozmen.products.adapter.out.mongodb.repository;

import com.zyozmen.products.adapter.out.mongodb.document.UserMongoDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Repositorio Spring Data MongoDB para usuarios.
 * El dominio no lo conoce en absoluto; es un detalle de infraestructura.
 */
public interface UserMongoRepository extends MongoRepository<UserMongoDocument, String> {

    boolean existsByUsername(String username);

    boolean existsByTipoIdentificacionAndNumeroIdentificacion(String tipoIdentificacion, String numeroIdentificacion);

    Optional<UserMongoDocument> findByUsername(String username);
}

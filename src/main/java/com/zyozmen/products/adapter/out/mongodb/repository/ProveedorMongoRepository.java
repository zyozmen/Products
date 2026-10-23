package com.zyozmen.products.adapter.out.mongodb.repository;

import com.zyozmen.products.adapter.out.mongodb.document.ProveedorDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProveedorMongoRepository extends MongoRepository<ProveedorDocument, String> {
    boolean existsByNit(String nit);

    @Query("{ '$or': [ { 'nombre': { '$regex': ?0, '$options': 'i' } }, "
            + "{ 'nit': { '$regex': ?0, '$options': 'i' } } ] }")
    Page<ProveedorDocument> buscarPorNombreONit(String patronLiteral, Pageable pageable);
}

package com.zyozmen.products.adapter.out.mongodb.repository;

import com.zyozmen.products.adapter.out.mongodb.document.ProveedorDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProveedorMongoRepository extends MongoRepository<ProveedorDocument, String> {
    boolean existsByNit(String nit);
}

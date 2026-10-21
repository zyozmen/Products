package com.zyozmen.products.adapter.out.mongodb.repository;

import com.zyozmen.products.adapter.out.mongodb.document.TransaccionProveedorDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TransaccionProveedorMongoRepository
        extends MongoRepository<TransaccionProveedorDocument, String> {
}

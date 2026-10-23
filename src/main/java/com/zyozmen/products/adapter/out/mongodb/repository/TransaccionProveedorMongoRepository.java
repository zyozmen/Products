package com.zyozmen.products.adapter.out.mongodb.repository;

import com.zyozmen.products.adapter.out.mongodb.document.TransaccionProveedorDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.bson.types.ObjectId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransaccionProveedorMongoRepository
        extends MongoRepository<TransaccionProveedorDocument, String> {
    @Query(value = "{ 'proveedorId': ?0, 'tipo': { $in: ['ABONO_INICIAL', 'ABONO', 'PAGO_CORTESIA', 'AJUSTE_PQR'] } }",
            sort = "{ 'fecha': -1, '_id': -1 }")
    Page<TransaccionProveedorDocument> findAbonos(ObjectId proveedorId, Pageable pageable);
}

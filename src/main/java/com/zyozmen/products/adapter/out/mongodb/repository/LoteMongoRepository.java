package com.zyozmen.products.adapter.out.mongodb.repository;

import com.zyozmen.products.adapter.out.mongodb.document.LoteDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface LoteMongoRepository extends MongoRepository<LoteDocument, String> {
    @Query(value = "{ 'proveedorId': ?0, 'saldoPendienteLote': { $gt: 0 } }", sort = "{ 'fechaIngreso': 1, '_id': 1 }")
    List<LoteDocument> findPendientes(ObjectId proveedorId);
}

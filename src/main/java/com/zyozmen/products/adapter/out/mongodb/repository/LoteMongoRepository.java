package com.zyozmen.products.adapter.out.mongodb.repository;

import com.zyozmen.products.adapter.out.mongodb.document.LoteDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LoteMongoRepository extends MongoRepository<LoteDocument, String> {
    @Query(value = "{ 'proveedorId': ?0, 'saldoPendienteLote': 0 }", count = true)
    long countPagados(ObjectId proveedorId);

    @Query(value = "{ 'proveedorId': ?0, 'saldoPendienteLote': { '$gt': 0 } }", count = true)
    long countPorPagar(ObjectId proveedorId);

    @Query("{ 'proveedorId': ?0 }")
    Page<LoteDocument> findLotes(ObjectId proveedorId, Pageable pageable);

    @Query("{ 'proveedorId': ?0, '$or': ["
            + "{ 'contenedores.cepa.nombre': { '$regex': ?1, '$options': 'i' } },"
            + "{ 'contenedores.presentaciones.nombre': { '$regex': ?1, '$options': 'i' } },"
            + "{ 'contenedores.presentaciones.sku': { '$regex': ?1, '$options': 'i' } }"
            + "] }")
    Page<LoteDocument> buscarLotes(ObjectId proveedorId, String patronLiteral, Pageable pageable);

    @Query(value = "{ 'proveedorId': ?0, 'saldoPendienteLote': { $gt: 0 } }", sort = "{ 'fechaRecepcion': 1, '_id': 1 }")
    List<LoteDocument> findPendientes(ObjectId proveedorId);
}

package com.zyozmen.products.adapter.out.mongodb.repository;

import com.zyozmen.products.adapter.out.mongodb.document.RevokedTokenMongoDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RevokedTokenMongoRepository extends MongoRepository<RevokedTokenMongoDocument, String> {
}

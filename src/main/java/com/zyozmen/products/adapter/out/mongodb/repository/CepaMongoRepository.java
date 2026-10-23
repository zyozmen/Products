package com.zyozmen.products.adapter.out.mongodb.repository;

import com.zyozmen.products.adapter.out.mongodb.document.CepaDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CepaMongoRepository extends MongoRepository<CepaDocument, String> { }

package com.zyozmen.products.adapter.out.mongodb.repository;

import com.zyozmen.products.adapter.out.mongodb.document.ProductDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductMongoRepository extends MongoRepository<ProductDocument, String> { }

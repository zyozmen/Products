package com.zyozmen.products.adapter.out.mongodb.repository;

import com.zyozmen.products.adapter.out.mongodb.document.OrderMongoDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrderMongoRepository extends MongoRepository<OrderMongoDocument, String> {
    List<OrderMongoDocument> findByUsernameOrderByCreatedAtDesc(String username);
}

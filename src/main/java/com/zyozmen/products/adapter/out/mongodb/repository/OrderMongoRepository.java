package com.zyozmen.products.adapter.out.mongodb.repository;

import com.zyozmen.products.adapter.out.mongodb.document.OrderDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrderMongoRepository extends MongoRepository<OrderDocument, String> {
    List<OrderDocument> findByUsernameOrderByCreatedAtDesc(String username);
}

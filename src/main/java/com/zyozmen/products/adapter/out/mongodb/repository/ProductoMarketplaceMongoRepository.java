package com.zyozmen.products.adapter.out.mongodb.repository;

import com.zyozmen.products.adapter.out.mongodb.document.ProductoMarketplaceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductoMarketplaceMongoRepository extends MongoRepository<ProductoMarketplaceDocument, String> {
    boolean existsBySku(String sku);

    boolean existsByPresentacionId(String presentacionId);
}

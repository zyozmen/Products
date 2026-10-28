package com.zyozmen.products.adapter.out.mongodb.repository;

import com.zyozmen.products.adapter.out.mongodb.document.ProductMarketplaceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface ProductMarketplaceMongoRepository extends MongoRepository<ProductMarketplaceDocument, String> {
    boolean existsBySku(String sku);

    boolean existsByPresentacionId(String presentacionId);

    Optional<ProductMarketplaceDocument> findByPresentacionId(String presentacionId);

    List<ProductMarketplaceDocument> findByCepaIdAndEstadoAndStockDisponibleGreaterThan(
            String cepaId, String estado, int stock);
}

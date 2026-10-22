package com.zyozmen.products.adapter.out.mongodb.repository;

import com.zyozmen.products.adapter.out.mongodb.document.ProductoMarketplaceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ProductoMarketplaceMongoRepository extends MongoRepository<ProductoMarketplaceDocument, String> {
    boolean existsBySku(String sku);

    boolean existsByPresentacionId(String presentacionId);

    Optional<ProductoMarketplaceDocument> findByPresentacionId(String presentacionId);

    List<ProductoMarketplaceDocument> findByCepaIdAndEstadoAndStockDisponibleGreaterThan(
            String cepaId, String estado, int stock);
}

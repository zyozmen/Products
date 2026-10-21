package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.ProductoMarketplace;

public interface MarketplaceRepositoryPort {
    boolean existsBySku(String sku);

    boolean existsByPresentacionId(String presentacionId);

    void insert(ProductoMarketplace producto);
}

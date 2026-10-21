package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.ProductoMarketplaceDocument;
import com.zyozmen.products.adapter.out.mongodb.mapper.ImagenMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.ProductoMarketplaceMongoRepository;
import com.zyozmen.products.domain.model.ProductoMarketplace;
import com.zyozmen.products.domain.port.out.MarketplaceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketplaceMongoAdapter implements MarketplaceRepositoryPort {
    private final ProductoMarketplaceMongoRepository repository;

    @Override
    public boolean existsBySku(String sku) {
        return repository.existsBySku(sku);
    }

    @Override
    public boolean existsByPresentacionId(String presentacionId) {
        return repository.existsByPresentacionId(presentacionId);
    }

    @Override
    public void insert(ProductoMarketplace p) {
        repository.insert(new ProductoMarketplaceDocument(null, p.presentacionId(), p.loteId(), p.proveedorNombre(),
                p.nombreProducto(), p.cepaNombre(), p.sku(), p.precioVenta(), p.stockDisponible(),
                ImagenMongoMapper.toDocument(p.fotografia()), p.estado()));
    }
}

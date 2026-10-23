package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.ProductoMarketplace;
import com.zyozmen.products.domain.model.MarketplaceCatalogFilter;
import java.util.List;
import java.util.Optional;
import com.zyozmen.products.domain.port.in.MarketplaceUseCase.ProductoCatalogo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MarketplaceRepositoryPort {
    boolean existsBySku(String sku);

    boolean existsByPresentacionId(String presentacionId);

    void insert(ProductoMarketplace producto);

    Optional<ProductoMarketplace> findByPresentacionId(String presentacionId);

    List<ProductoMarketplace> findDisponiblesByCepaId(String cepaId);

    Page<ProductoCatalogo> buscarCatalogo(String buscar, Pageable pageable);
    Page<ProductoCatalogo> buscarCatalogo(MarketplaceCatalogFilter filtro, Pageable pageable);

    void actualizarStock(String presentacionId, String loteId, int anterior, int nuevo);
}

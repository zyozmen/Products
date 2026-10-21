package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.CatalogCategory;

import java.util.List;
import java.util.Optional;

public interface CategoryRepositoryPort {
    CatalogCategory save(CatalogCategory category);
    Optional<CatalogCategory> findById(Long id);
    List<CatalogCategory> findAll();
}

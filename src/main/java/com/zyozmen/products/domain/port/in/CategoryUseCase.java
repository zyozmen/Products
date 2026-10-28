package com.zyozmen.products.domain.port.in;

import com.zyozmen.products.domain.model.CatalogCategory;
import com.zyozmen.products.domain.model.Image;

import java.util.List;

public interface CategoryUseCase {
    CatalogCategory createCategory(CatalogCategory category);
    CatalogCategory updateCategory(Long id, CatalogCategory category);
    List<CatalogCategory> getAllCategories();
    CatalogCategory getCategoryById(Long id);
    CatalogCategory saveImage(Long id, Image image);
}

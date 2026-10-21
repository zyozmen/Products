package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.CatalogCategory;
import com.zyozmen.products.domain.model.Imagen;
import com.zyozmen.products.domain.port.in.CategoryUseCase;
import com.zyozmen.products.domain.port.out.CategoryRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService implements CategoryUseCase {
    private final CategoryRepositoryPort repository;

    @Override
    public CatalogCategory createCategory(CatalogCategory category) {
        category.setCategoryId(null);
        category.setName(category.getName().strip());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        return repository.save(category);
    }

    @Override
    public CatalogCategory updateCategory(Long id, CatalogCategory category) {
        CatalogCategory existing = getCategoryById(id);
        existing.setName(category.getName().strip());
        existing.setDescription(category.getDescription());
        if (category.getImageUrl() != null) {
            existing.setImageUrl(category.getImageUrl());
            existing.setImage(null);
        }
        existing.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        return repository.save(existing);
    }

    @Override
    public List<CatalogCategory> getAllCategories() {
        return repository.findAll();
    }

    @Override
    public CatalogCategory getCategoryById(Long id) {
        return repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Categoria no encontrada con ID: " + id));
    }

    @Override
    public CatalogCategory saveImage(Long id, Imagen image) {
        if (image == null) {
            throw new IllegalArgumentException("La foto principal es obligatoria para categorias");
        }
        CatalogCategory existing = getCategoryById(id);
        existing.setImage(image);
        existing.setImageUrl("/api/v1/categories/" + id + "/image");
        existing.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        return repository.save(existing);
    }
}

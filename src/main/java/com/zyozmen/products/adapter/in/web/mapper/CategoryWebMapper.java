package com.zyozmen.products.adapter.in.web.mapper;

import com.zyozmen.products.adapter.in.web.dto.CategoryRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.CategoryResponseDTO;
import com.zyozmen.products.domain.model.CatalogCategory;
import org.springframework.stereotype.Component;

@Component
public class CategoryWebMapper {
    public CatalogCategory toDomain(CategoryRequestDTO request) {
        return CatalogCategory.builder().name(request.name()).description(request.description())
                .imageUrl(request.imageUrl()).build();
    }

    public CategoryResponseDTO toResponse(CatalogCategory category) {
        return new CategoryResponseDTO(category.getCategoryId(), category.getName(),
                category.getDescription(), category.getImageUrl());
    }
}

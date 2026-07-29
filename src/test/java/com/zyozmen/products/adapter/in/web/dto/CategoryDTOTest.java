package com.zyozmen.products.adapter.in.web.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryDTOTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        CategoryDTO dto = CategoryDTO.builder()
                .categoryId(1L)
                .name("Electronics")
                .slug("electronics")
                .productsCount(42L)
                .build();

        assertThat(dto.getCategoryId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Electronics");
        assertThat(dto.getSlug()).isEqualTo("electronics");
        assertThat(dto.getProductsCount()).isEqualTo(42L);
    }
}

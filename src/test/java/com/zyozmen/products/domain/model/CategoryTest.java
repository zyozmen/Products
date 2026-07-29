package com.zyozmen.products.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        Category category = Category.builder()
                .categoryId(1L)
                .name("Electronics")
                .slug("electronics")
                .productsCount(42L)
                .build();

        assertThat(category.getCategoryId()).isEqualTo(1L);
        assertThat(category.getName()).isEqualTo("Electronics");
        assertThat(category.getSlug()).isEqualTo("electronics");
        assertThat(category.getProductsCount()).isEqualTo(42L);
    }
}

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

    @Test
    void settersAndEqualityShouldWork() {
        Category category = new Category();
        category.setCategoryId(2L);
        category.setName("Gaming");
        category.setSlug("gaming");
        category.setProductsCount(7L);

        Category sameCategory = Category.builder()
                .categoryId(2L)
                .name("Gaming")
                .slug("gaming")
                .productsCount(7L)
                .build();
        Category differentCategory = Category.builder()
                .categoryId(3L)
                .name("Gaming")
                .slug("gaming")
                .productsCount(7L)
                .build();

        assertThat(category).isEqualTo(sameCategory);
        assertThat(category.hashCode()).isEqualTo(sameCategory.hashCode());
        assertThat(category).isNotEqualTo(differentCategory);
        assertThat(category.toString()).contains("Gaming");
    }
}

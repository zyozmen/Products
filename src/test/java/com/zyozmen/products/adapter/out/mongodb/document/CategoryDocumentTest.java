package com.zyozmen.products.adapter.out.mongodb.document;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryDocumentTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        CategoryDocument document = CategoryDocument.builder()
                .categoryId(1L)
                .name("Electronics")
                .slug("electronics")
                .productsCount(42L)
                .build();

        assertThat(document.getCategoryId()).isEqualTo(1L);
        assertThat(document.getName()).isEqualTo("Electronics");
        assertThat(document.getSlug()).isEqualTo("electronics");
        assertThat(document.getProductsCount()).isEqualTo(42L);
    }
}

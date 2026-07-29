package com.zyozmen.products.adapter.out.mongodb.repository;

import com.zyozmen.products.adapter.out.mongodb.document.CategoryDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProductoMongoDocument;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductoMongoRepositoryTest {

    @Test
    void shouldCreatePageableQueryResult() {
        ProductoMongoDocument document = ProductoMongoDocument.builder()
                .id("1")
                .name("Wireless Headphones")
                .build();

        Page<ProductoMongoDocument> page = new PageImpl<>(List.of(document), Pageable.unpaged(), 1);

        assertThat(page.getContent()).containsExactly(document);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldCreateCategoryDocumentsList() {
        CategoryDocument category = CategoryDocument.builder()
                .categoryId(1L)
                .name("Electronics")
                .slug("electronics")
                .productsCount(42L)
                .build();

        List<CategoryDocument> categories = List.of(category);

        assertThat(categories).containsExactly(category);
        assertThat(categories.get(0).getName()).isEqualTo("Electronics");
    }
}

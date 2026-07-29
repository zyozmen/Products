package com.zyozmen.products.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductoTest {

    @Test
    void builderShouldPopulateFieldsAndLegacyAliases() {
        Instant createdAt = Instant.parse("2025-11-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-06-17T13:00:00Z");

        Producto producto = Producto.builder()
                .id("1")
                .name("Wireless Headphones")
                .slug("wireless-headphones")
                .description("Great sound")
                .sku("SKU-001")
                .status("active")
                .categories(List.of(Category.builder().categoryId(1L).name("Audio").build()))
                .price(Price.builder().current(new BigDecimal("199.99")).build())
                .ranking(Ranking.builder().build())
                .recentComments(List.of(Comment.builder().commentId("comment-1").build()))
                .hasMoreComments(true)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        assertThat(producto.getId()).isEqualTo("1");
        assertThat(producto.getName()).isEqualTo("Wireless Headphones");
        assertThat(producto.getSlug()).isEqualTo("wireless-headphones");
        assertThat(producto.getDescription()).isEqualTo("Great sound");
        assertThat(producto.getSku()).isEqualTo("SKU-001");
        assertThat(producto.getStatus()).isEqualTo("active");
        assertThat(producto.getCategories()).hasSize(1);
        assertThat(producto.getPrice().getCurrent()).isEqualByComparingTo("199.99");
        assertThat(producto.getRanking()).isNotNull();
        assertThat(producto.getRecentComments()).hasSize(1);
        assertThat(producto.getHasMoreComments()).isTrue();
        assertThat(producto.getCreatedAt()).isEqualTo(createdAt);
        assertThat(producto.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(producto.getNombre()).isEqualTo("Wireless Headphones");
        assertThat(producto.getPrecio()).isEqualByComparingTo("199.99");
    }
}

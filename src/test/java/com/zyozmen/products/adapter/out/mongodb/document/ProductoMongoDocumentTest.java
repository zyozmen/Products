package com.zyozmen.products.adapter.out.mongodb.document;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductoMongoDocumentTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        Instant createdAt = Instant.parse("2025-11-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-06-17T13:00:00Z");

        ProductoMongoDocument document = ProductoMongoDocument.builder()
                .id("1")
                .name("Wireless Headphones")
                .slug("wireless-headphones")
                .description("Great sound")
                .sku("SKU-001")
                .status("active")
                .categories(List.of(CategoryDocument.builder().categoryId(1L).name("Audio").build()))
                .price(PriceDocument.builder().currency("USD").build())
                .ranking(RankingDocument.builder().build())
                .recentComments(List.of(CommentDocument.builder().commentId("comment-1").build()))
                .hasMoreComments(true)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        assertThat(document.getId()).isEqualTo("1");
        assertThat(document.getName()).isEqualTo("Wireless Headphones");
        assertThat(document.getSlug()).isEqualTo("wireless-headphones");
        assertThat(document.getDescription()).isEqualTo("Great sound");
        assertThat(document.getSku()).isEqualTo("SKU-001");
        assertThat(document.getStatus()).isEqualTo("active");
        assertThat(document.getCategories()).hasSize(1);
        assertThat(document.getPrice()).isNotNull();
        assertThat(document.getRanking()).isNotNull();
        assertThat(document.getRecentComments()).hasSize(1);
        assertThat(document.getHasMoreComments()).isTrue();
        assertThat(document.getCreatedAt()).isEqualTo(createdAt);
        assertThat(document.getUpdatedAt()).isEqualTo(updatedAt);
    }
}

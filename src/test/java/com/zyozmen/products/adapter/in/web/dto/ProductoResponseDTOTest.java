package com.zyozmen.products.adapter.in.web.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductoResponseDTOTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        Instant createdAt = Instant.parse("2025-11-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-06-17T13:00:00Z");

        ProductoResponseDTO dto = ProductoResponseDTO.builder()
                .id("1")
                .name("Wireless Headphones")
                .slug("wireless-headphones")
                .description("Great sound")
                .sku("SKU-001")
                .status("active")
                .categories(List.of(CategoryDTO.builder().categoryId(1L).name("Audio").build()))
                .price(PriceDTO.builder().currency("USD").build())
                .ranking(RankingDTO.builder().build())
                .recentComments(List.of(RecentCommentDTO.builder().commentId("comment-1").build()))
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        assertThat(dto.getId()).isEqualTo("1");
        assertThat(dto.getName()).isEqualTo("Wireless Headphones");
        assertThat(dto.getSlug()).isEqualTo("wireless-headphones");
        assertThat(dto.getDescription()).isEqualTo("Great sound");
        assertThat(dto.getSku()).isEqualTo("SKU-001");
        assertThat(dto.getStatus()).isEqualTo("active");
        assertThat(dto.getCategories()).hasSize(1);
        assertThat(dto.getPrice()).isNotNull();
        assertThat(dto.getRanking()).isNotNull();
        assertThat(dto.getRecentComments()).hasSize(1);
        assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
        assertThat(dto.getUpdatedAt()).isEqualTo(updatedAt);
    }
}

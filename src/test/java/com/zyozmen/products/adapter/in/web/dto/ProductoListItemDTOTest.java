package com.zyozmen.products.adapter.in.web.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductoListItemDTOTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        ProductoListItemDTO dto = ProductoListItemDTO.builder()
                .id("1")
                .name("Wireless Headphones")
                .categoryIds(List.of(1L, 2L))
                .currentPrice(new BigDecimal("199.99"))
                .originalPrice(new BigDecimal("249.99"))
                .priceCurrency("USD")
                .averageRating(new BigDecimal("4.7"))
                .totalReviews(128)
                .build();

        assertThat(dto.getId()).isEqualTo("1");
        assertThat(dto.getName()).isEqualTo("Wireless Headphones");
        assertThat(dto.getCategoryIds()).containsExactly(1L, 2L);
        assertThat(dto.getCurrentPrice()).isEqualByComparingTo("199.99");
        assertThat(dto.getOriginalPrice()).isEqualByComparingTo("249.99");
        assertThat(dto.getPriceCurrency()).isEqualTo("USD");
        assertThat(dto.getAverageRating()).isEqualByComparingTo("4.7");
        assertThat(dto.getTotalReviews()).isEqualTo(128);
    }
}

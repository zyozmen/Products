package com.zyozmen.products.adapter.in.web.dto;

import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductoRequestDTOTest {

        private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        CategoryDTO category = CategoryDTO.builder()
                .categoryId(1L)
                .name("Audio")
                .build();

        PriceDTO price = PriceDTO.builder()
                .current(new BigDecimal("199.99"))
                .original(new BigDecimal("249.99"))
                .currency("USD")
                .discountPercentage(20)
                .taxInclusive(true)
                .build();

        RankingDTO ranking = RankingDTO.builder()
                .averageRating(new BigDecimal("4.7"))
                .totalReviews(128)
                .build();

        RecentCommentDTO comment = RecentCommentDTO.builder()
                .commentId("comment-1")
                .userId("user-1")
                .username("JohnDoe")
                .rating(5)
                .title("Amazing")
                .body("Excellent")
                .build();

        ProductoRequestDTO dto = ProductoRequestDTO.builder()
                .id("1")
                .name("Wireless Headphones")
                .slug("wireless-headphones")
                .description("Great sound")
                .sku("SKU-001")
                .status("active")
                .categories(List.of(category))
                .price(price)
                .ranking(ranking)
                .recentComments(List.of(comment))
                .hasMoreComments(true)
                .build();

        assertThat(dto.getId()).isEqualTo("1");
        assertThat(dto.getName()).isEqualTo("Wireless Headphones");
        assertThat(dto.getSlug()).isEqualTo("wireless-headphones");
        assertThat(dto.getDescription()).isEqualTo("Great sound");
        assertThat(dto.getSku()).isEqualTo("SKU-001");
        assertThat(dto.getStatus()).isEqualTo("active");
        assertThat(dto.getCategories()).containsExactly(category);
        assertThat(dto.getPrice()).isEqualTo(price);
        assertThat(dto.getRanking()).isEqualTo(ranking);
        assertThat(dto.getRecentComments()).containsExactly(comment);
        assertThat(dto.getHasMoreComments()).isTrue();
    }

        @Test
        void categoriesShouldRequireAtLeastOneCategory() {
                ProductoRequestDTO dto = ProductoRequestDTO.builder()
                                .name("Product")
                                .slug("product")
                                .sku("SKU-001")
                                .status("active")
                                .categories(List.of())
                                .build();

                assertThat(validator.validateProperty(dto, "categories"))
                                .extracting("message")
                                .contains("El producto debe tener al menos una categoría");
        }
}

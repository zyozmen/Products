package com.zyozmen.products.adapter.out.mongodb.document;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewDocumentTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        ReviewDocument document = ReviewDocument.builder()
                .autor("John")
                .stars(new BigDecimal("4.5"))
                .review("Great product")
                .email("john@example.com")
                .build();

        assertThat(document.getAutor()).isEqualTo("John");
        assertThat(document.getStars()).isEqualByComparingTo("4.5");
        assertThat(document.getReview()).isEqualTo("Great product");
        assertThat(document.getEmail()).isEqualTo("john@example.com");
    }
}

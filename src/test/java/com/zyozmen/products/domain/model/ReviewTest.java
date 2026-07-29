package com.zyozmen.products.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        Review review = Review.builder()
                .autor("John")
                .stars(new BigDecimal("4.5"))
                .reviewText("Great product")
                .email("john@example.com")
                .build();

        assertThat(review.getAutor()).isEqualTo("John");
        assertThat(review.getStars()).isEqualByComparingTo("4.5");
        assertThat(review.getReviewText()).isEqualTo("Great product");
        assertThat(review.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void settersAndEqualityShouldWork() {
        Review review = new Review();
        review.setAutor("Jane");
        review.setStars(new BigDecimal("3.5"));
        review.setReviewText("Good enough");
        review.setEmail("jane@example.com");

        Review sameReview = Review.builder()
                .autor("Jane")
                .stars(new BigDecimal("3.5"))
                .reviewText("Good enough")
                .email("jane@example.com")
                .build();

        assertThat(review).isEqualTo(sameReview);
        assertThat(review.hashCode()).isEqualTo(sameReview.hashCode());
        assertThat(review.toString()).contains("Jane");
    }
}

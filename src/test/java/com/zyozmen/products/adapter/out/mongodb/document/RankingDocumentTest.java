package com.zyozmen.products.adapter.out.mongodb.document;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RankingDocumentTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        RatingDistributionDocument distribution = RatingDistributionDocument.builder()
                .fiveStar(900)
                .fourStar(250)
                .threeStar(70)
                .twoStar(20)
                .oneStar(10)
                .build();

        RankingDocument document = RankingDocument.builder()
                .averageRating(new BigDecimal("4.7"))
                .totalReviews(1250)
                .ratingDistribution(distribution)
                .build();

        assertThat(document.getAverageRating()).isEqualByComparingTo("4.7");
        assertThat(document.getTotalReviews()).isEqualTo(1250);
        assertThat(document.getRatingDistribution()).isEqualTo(distribution);
    }
}

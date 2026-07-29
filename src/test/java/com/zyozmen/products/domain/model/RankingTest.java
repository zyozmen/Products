package com.zyozmen.products.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RankingTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        RatingDistribution distribution = RatingDistribution.builder()
                .fiveStar(900)
                .fourStar(250)
                .threeStar(70)
                .twoStar(20)
                .oneStar(10)
                .build();

        Ranking ranking = Ranking.builder()
                .averageRating(new BigDecimal("4.7"))
                .totalReviews(1250)
                .ratingDistribution(distribution)
                .build();

        assertThat(ranking.getAverageRating()).isEqualByComparingTo("4.7");
        assertThat(ranking.getTotalReviews()).isEqualTo(1250);
        assertThat(ranking.getRatingDistribution()).isEqualTo(distribution);
    }

    @Test
    void settersAndEqualityShouldWork() {
        Ranking ranking = new Ranking();
        ranking.setAverageRating(new BigDecimal("4.2"));
        ranking.setTotalReviews(200);
        ranking.setRatingDistribution(RatingDistribution.builder().fiveStar(100).build());

        Ranking sameRanking = Ranking.builder()
                .averageRating(new BigDecimal("4.2"))
                .totalReviews(200)
                .ratingDistribution(RatingDistribution.builder().fiveStar(100).build())
                .build();

        assertThat(ranking).isEqualTo(sameRanking);
        assertThat(ranking.hashCode()).hasSameHashCodeAs(sameRanking.hashCode());
        assertThat(ranking.toString()).contains("4.2");
    }
}

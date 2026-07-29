package com.zyozmen.products.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RatingDistributionTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        RatingDistribution distribution = RatingDistribution.builder()
                .fiveStar(900)
                .fourStar(250)
                .threeStar(70)
                .twoStar(20)
                .oneStar(10)
                .build();

        assertThat(distribution.getFiveStar()).isEqualTo(900);
        assertThat(distribution.getFourStar()).isEqualTo(250);
        assertThat(distribution.getThreeStar()).isEqualTo(70);
        assertThat(distribution.getTwoStar()).isEqualTo(20);
        assertThat(distribution.getOneStar()).isEqualTo(10);
    }
}

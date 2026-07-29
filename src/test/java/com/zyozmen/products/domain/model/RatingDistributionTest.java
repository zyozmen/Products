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

    @Test
    void settersAndEqualityShouldWork() {
        RatingDistribution distribution = new RatingDistribution();
        distribution.setFiveStar(1);
        distribution.setFourStar(2);
        distribution.setThreeStar(3);
        distribution.setTwoStar(4);
        distribution.setOneStar(5);

        RatingDistribution sameDistribution = RatingDistribution.builder()
                .fiveStar(1)
                .fourStar(2)
                .threeStar(3)
                .twoStar(4)
                .oneStar(5)
                .build();

        assertThat(distribution).isEqualTo(sameDistribution);
        assertThat(distribution.hashCode()).hasSameHashCodeAs(sameDistribution.hashCode());
        assertThat(distribution.toString()).contains("5");
    }
}

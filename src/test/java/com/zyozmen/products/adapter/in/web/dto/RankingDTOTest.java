package com.zyozmen.products.adapter.in.web.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RankingDTOTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        RatingDistributionDTO distribution = RatingDistributionDTO.builder()
                .fiveStar(900)
                .fourStar(250)
                .threeStar(70)
                .twoStar(20)
                .oneStar(10)
                .build();

        RankingDTO dto = RankingDTO.builder()
                .averageRating(new BigDecimal("4.7"))
                .totalReviews(1250)
                .ratingDistribution(distribution)
                .build();

        assertThat(dto.getAverageRating()).isEqualByComparingTo("4.7");
        assertThat(dto.getTotalReviews()).isEqualTo(1250);
        assertThat(dto.getRatingDistribution()).isEqualTo(distribution);
    }
}

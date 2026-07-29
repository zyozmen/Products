package com.zyozmen.products.adapter.in.web.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RatingDistributionDTOTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        RatingDistributionDTO dto = RatingDistributionDTO.builder()
                .fiveStar(900)
                .fourStar(250)
                .threeStar(70)
                .twoStar(20)
                .oneStar(10)
                .build();

        assertThat(dto.getFiveStar()).isEqualTo(900);
        assertThat(dto.getFourStar()).isEqualTo(250);
        assertThat(dto.getThreeStar()).isEqualTo(70);
        assertThat(dto.getTwoStar()).isEqualTo(20);
        assertThat(dto.getOneStar()).isEqualTo(10);
    }
}

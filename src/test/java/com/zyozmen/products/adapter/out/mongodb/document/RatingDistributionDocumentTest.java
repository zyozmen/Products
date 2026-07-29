package com.zyozmen.products.adapter.out.mongodb.document;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RatingDistributionDocumentTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        RatingDistributionDocument document = RatingDistributionDocument.builder()
                .fiveStar(900)
                .fourStar(250)
                .threeStar(70)
                .twoStar(20)
                .oneStar(10)
                .build();

        assertThat(document.getFiveStar()).isEqualTo(900);
        assertThat(document.getFourStar()).isEqualTo(250);
        assertThat(document.getThreeStar()).isEqualTo(70);
        assertThat(document.getTwoStar()).isEqualTo(20);
        assertThat(document.getOneStar()).isEqualTo(10);
    }
}

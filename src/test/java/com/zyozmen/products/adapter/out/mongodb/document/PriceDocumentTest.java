package com.zyozmen.products.adapter.out.mongodb.document;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PriceDocumentTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        PriceDocument document = PriceDocument.builder()
                .current(new BigDecimal("199.99"))
                .original(new BigDecimal("249.99"))
                .currency("USD")
                .discountPercentage(20)
                .taxInclusive(true)
                .build();

        assertThat(document.getCurrent()).isEqualByComparingTo("199.99");
        assertThat(document.getOriginal()).isEqualByComparingTo("249.99");
        assertThat(document.getCurrency()).isEqualTo("USD");
        assertThat(document.getDiscountPercentage()).isEqualTo(20);
        assertThat(document.getTaxInclusive()).isTrue();
    }
}

package com.zyozmen.products.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PriceTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        Price price = Price.builder()
                .current(new BigDecimal("199.99"))
                .original(new BigDecimal("249.99"))
                .currency("USD")
                .discountPercentage(20)
                .taxInclusive(true)
                .build();

        assertThat(price.getCurrent()).isEqualByComparingTo("199.99");
        assertThat(price.getOriginal()).isEqualByComparingTo("249.99");
        assertThat(price.getCurrency()).isEqualTo("USD");
        assertThat(price.getDiscountPercentage()).isEqualTo(20);
        assertThat(price.getTaxInclusive()).isTrue();
    }
}

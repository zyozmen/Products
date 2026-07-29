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

    @Test
    void settersAndEqualityShouldWork() {
        Price price = new Price();
        price.setCurrent(new BigDecimal("10.00"));
        price.setOriginal(new BigDecimal("15.00"));
        price.setCurrency("EUR");
        price.setDiscountPercentage(15);
        price.setTaxInclusive(false);

        Price samePrice = Price.builder()
                .current(new BigDecimal("10.00"))
                .original(new BigDecimal("15.00"))
                .currency("EUR")
                .discountPercentage(15)
                .taxInclusive(false)
                .build();

        assertThat(price).isEqualTo(samePrice);
        assertThat(price.hashCode()).hasSameHashCodeAs(samePrice.hashCode());
        assertThat(price.toString()).contains("EUR");
    }
}

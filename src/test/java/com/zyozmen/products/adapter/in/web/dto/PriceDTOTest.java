package com.zyozmen.products.adapter.in.web.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PriceDTOTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        PriceDTO dto = PriceDTO.builder()
                .current(new BigDecimal("199.99"))
                .original(new BigDecimal("249.99"))
                .currency("USD")
                .discountPercentage(20)
                .taxInclusive(true)
                .build();

        assertThat(dto.getCurrent()).isEqualByComparingTo("199.99");
        assertThat(dto.getOriginal()).isEqualByComparingTo("249.99");
        assertThat(dto.getCurrency()).isEqualTo("USD");
        assertThat(dto.getDiscountPercentage()).isEqualTo(20);
        assertThat(dto.getTaxInclusive()).isTrue();
    }
}

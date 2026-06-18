package com.zyozmen.products.adapter.out.mongodb.document;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceDocument {

    @Field("current")
    private BigDecimal current;

    @Field("original")
    private BigDecimal original;

    @Field("currency")
    private String currency;

    @Field("discount_percentage")
    private Integer discountPercentage;

    @Field("tax_inclusive")
    private Boolean taxInclusive;
}
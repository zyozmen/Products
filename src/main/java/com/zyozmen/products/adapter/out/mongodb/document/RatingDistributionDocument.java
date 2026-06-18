package com.zyozmen.products.adapter.out.mongodb.document;

import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingDistributionDocument {

    @Field("5_star")
    private Integer fiveStar;

    @Field("4_star")
    private Integer fourStar;

    @Field("3_star")
    private Integer threeStar;

    @Field("2_star")
    private Integer twoStar;

    @Field("1_star")
    private Integer oneStar;
}
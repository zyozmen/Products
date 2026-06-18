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
public class RankingDocument {

    @Field("average_rating")
    private BigDecimal averageRating;

    @Field("total_reviews")
    private Integer totalReviews;

    @Field("rating_distribution")
    private RatingDistributionDocument ratingDistribution;
}
package com.zyozmen.products.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Ranking agregado del producto.
 */
public class Ranking {

    private BigDecimal averageRating;
    private Integer totalReviews;
    private RatingDistribution ratingDistribution;

    public Ranking() {
    }

    public Ranking(BigDecimal averageRating, Integer totalReviews, RatingDistribution ratingDistribution) {
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.ratingDistribution = ratingDistribution;
    }

    public static Builder builder() {
        return new Builder();
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Integer totalReviews) {
        this.totalReviews = totalReviews;
    }

    public RatingDistribution getRatingDistribution() {
        return ratingDistribution;
    }

    public void setRatingDistribution(RatingDistribution ratingDistribution) {
        this.ratingDistribution = ratingDistribution;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ranking ranking = (Ranking) o;
        return Objects.equals(averageRating, ranking.averageRating)
                && Objects.equals(totalReviews, ranking.totalReviews)
                && Objects.equals(ratingDistribution, ranking.ratingDistribution);
    }

    @Override
    public int hashCode() {
        return Objects.hash(averageRating, totalReviews, ratingDistribution);
    }

    @Override
    public String toString() {
        return "Ranking{" +
                "averageRating=" + averageRating +
                ", totalReviews=" + totalReviews +
                ", ratingDistribution=" + ratingDistribution +
                '}';
    }

    public static class Builder {

        private BigDecimal averageRating;
        private Integer totalReviews;
        private RatingDistribution ratingDistribution;

        public Builder averageRating(BigDecimal averageRating) {
            this.averageRating = averageRating;
            return this;
        }

        public Builder totalReviews(Integer totalReviews) {
            this.totalReviews = totalReviews;
            return this;
        }

        public Builder ratingDistribution(RatingDistribution ratingDistribution) {
            this.ratingDistribution = ratingDistribution;
            return this;
        }

        public Ranking build() {
            return new Ranking(averageRating, totalReviews, ratingDistribution);
        }
    }
}

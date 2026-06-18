package com.zyozmen.products.domain.model;

import java.util.Objects;

/**
 * Distribución de calificaciones por estrellas.
 */
public class RatingDistribution {

    private Integer fiveStar;
    private Integer fourStar;
    private Integer threeStar;
    private Integer twoStar;
    private Integer oneStar;

    public RatingDistribution() {
    }

    public RatingDistribution(Integer fiveStar, Integer fourStar, Integer threeStar, Integer twoStar, Integer oneStar) {
        this.fiveStar = fiveStar;
        this.fourStar = fourStar;
        this.threeStar = threeStar;
        this.twoStar = twoStar;
        this.oneStar = oneStar;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer getFiveStar() {
        return fiveStar;
    }

    public void setFiveStar(Integer fiveStar) {
        this.fiveStar = fiveStar;
    }

    public Integer getFourStar() {
        return fourStar;
    }

    public void setFourStar(Integer fourStar) {
        this.fourStar = fourStar;
    }

    public Integer getThreeStar() {
        return threeStar;
    }

    public void setThreeStar(Integer threeStar) {
        this.threeStar = threeStar;
    }

    public Integer getTwoStar() {
        return twoStar;
    }

    public void setTwoStar(Integer twoStar) {
        this.twoStar = twoStar;
    }

    public Integer getOneStar() {
        return oneStar;
    }

    public void setOneStar(Integer oneStar) {
        this.oneStar = oneStar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RatingDistribution that = (RatingDistribution) o;
        return Objects.equals(fiveStar, that.fiveStar)
                && Objects.equals(fourStar, that.fourStar)
                && Objects.equals(threeStar, that.threeStar)
                && Objects.equals(twoStar, that.twoStar)
                && Objects.equals(oneStar, that.oneStar);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fiveStar, fourStar, threeStar, twoStar, oneStar);
    }

    @Override
    public String toString() {
        return "RatingDistribution{" +
                "fiveStar=" + fiveStar +
                ", fourStar=" + fourStar +
                ", threeStar=" + threeStar +
                ", twoStar=" + twoStar +
                ", oneStar=" + oneStar +
                '}';
    }

    public static class Builder {

        private Integer fiveStar;
        private Integer fourStar;
        private Integer threeStar;
        private Integer twoStar;
        private Integer oneStar;

        public Builder fiveStar(Integer fiveStar) {
            this.fiveStar = fiveStar;
            return this;
        }

        public Builder fourStar(Integer fourStar) {
            this.fourStar = fourStar;
            return this;
        }

        public Builder threeStar(Integer threeStar) {
            this.threeStar = threeStar;
            return this;
        }

        public Builder twoStar(Integer twoStar) {
            this.twoStar = twoStar;
            return this;
        }

        public Builder oneStar(Integer oneStar) {
            this.oneStar = oneStar;
            return this;
        }

        public RatingDistribution build() {
            return new RatingDistribution(fiveStar, fourStar, threeStar, twoStar, oneStar);
        }
    }
}

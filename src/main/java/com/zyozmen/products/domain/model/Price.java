package com.zyozmen.products.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Información de precio del producto.
 */
public class Price {

    private BigDecimal current;
    private BigDecimal original;
    private String currency;
    private Integer discountPercentage;
    private Boolean taxInclusive;

    public Price() {
    }

    public Price(BigDecimal current, BigDecimal original, String currency, Integer discountPercentage, Boolean taxInclusive) {
        this.current = current;
        this.original = original;
        this.currency = currency;
        this.discountPercentage = discountPercentage;
        this.taxInclusive = taxInclusive;
    }

    public static Builder builder() {
        return new Builder();
    }

    public BigDecimal getCurrent() {
        return current;
    }

    public void setCurrent(BigDecimal current) {
        this.current = current;
    }

    public BigDecimal getOriginal() {
        return original;
    }

    public void setOriginal(BigDecimal original) {
        this.original = original;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(Integer discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public Boolean getTaxInclusive() {
        return taxInclusive;
    }

    public void setTaxInclusive(Boolean taxInclusive) {
        this.taxInclusive = taxInclusive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Price price = (Price) o;
        return Objects.equals(current, price.current)
                && Objects.equals(original, price.original)
                && Objects.equals(currency, price.currency)
                && Objects.equals(discountPercentage, price.discountPercentage)
                && Objects.equals(taxInclusive, price.taxInclusive);
    }

    @Override
    public int hashCode() {
        return Objects.hash(current, original, currency, discountPercentage, taxInclusive);
    }

    @Override
    public String toString() {
        return "Price{" +
                "current=" + current +
                ", original=" + original +
                ", currency='" + currency + '\'' +
                ", discountPercentage=" + discountPercentage +
                ", taxInclusive=" + taxInclusive +
                '}';
    }

    public static class Builder {

        private BigDecimal current;
        private BigDecimal original;
        private String currency;
        private Integer discountPercentage;
        private Boolean taxInclusive;

        public Builder current(BigDecimal current) {
            this.current = current;
            return this;
        }

        public Builder original(BigDecimal original) {
            this.original = original;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder discountPercentage(Integer discountPercentage) {
            this.discountPercentage = discountPercentage;
            return this;
        }

        public Builder taxInclusive(Boolean taxInclusive) {
            this.taxInclusive = taxInclusive;
            return this;
        }

        public Price build() {
            return new Price(current, original, currency, discountPercentage, taxInclusive);
        }
    }
}

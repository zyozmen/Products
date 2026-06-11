package com.zyozmen.products.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Objeto de valor del dominio que representa una reseña de producto.
 * No tiene identidad propia; pertenece al agregado Producto.
 */
public class Review {

    private String autor;
    private BigDecimal stars;
    private String review;
    private String email;

    public Review() {
    }

    public Review(String autor, BigDecimal stars, String review, String email) {
        this.autor = autor;
        this.stars = stars;
        this.review = review;
        this.email = email;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public BigDecimal getStars() {
        return stars;
    }

    public void setStars(BigDecimal stars) {
        this.stars = stars;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Review r = (Review) o;
        return Objects.equals(autor, r.autor)
                && Objects.equals(stars, r.stars)
                && Objects.equals(review, r.review)
                && Objects.equals(email, r.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(autor, stars, review, email);
    }

    @Override
    public String toString() {
        return "Review{" +
                "autor='" + autor + '\'' +
                ", stars=" + stars +
                ", review='" + review + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

    public static class Builder {

        private String autor;
        private BigDecimal stars;
        private String review;
        private String email;

        public Builder autor(String autor) {
            this.autor = autor;
            return this;
        }

        public Builder stars(BigDecimal stars) {
            this.stars = stars;
            return this;
        }

        public Builder review(String review) {
            this.review = review;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Review build() {
            return new Review(autor, stars, review, email);
        }
    }
}

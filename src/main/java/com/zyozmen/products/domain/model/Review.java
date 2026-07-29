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
    private String reviewText;
    private String email;

    public Review() {
    }

    public Review(String autor, BigDecimal stars, String reviewText, String email) {
        this.autor = autor;
        this.stars = stars;
        this.reviewText = reviewText;
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

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
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
                && Objects.equals(reviewText, r.reviewText)
                && Objects.equals(email, r.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(autor, stars, reviewText, email);
    }

    @Override
    public String toString() {
        return "Review{" +
                "autor='" + autor + '\'' +
                ", stars=" + stars +
                ", review='" + reviewText + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

    public static class Builder {

        private String autor;
        private BigDecimal stars;
        private String reviewText;
        private String email;

        public Builder autor(String autor) {
            this.autor = autor;
            return this;
        }

        public Builder stars(BigDecimal stars) {
            this.stars = stars;
            return this;
        }

        public Builder reviewText(String reviewText) {
            this.reviewText = reviewText;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Review build() {
            return new Review(autor, stars, reviewText, email);
        }
    }
}

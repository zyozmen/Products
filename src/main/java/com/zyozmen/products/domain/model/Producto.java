package com.zyozmen.products.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Modelo de dominio Producto. 
 * 
 */
public class Producto {

    private String id;
    private String name;
    private String slug;
    private String description;
    private String sku;
    private String status;
    private List<Category> categories;
    private Price price;
    private Ranking ranking;
    private List<Comment> recentComments;
    private Boolean hasMoreComments;
    private Instant createdAt;
    private Instant updatedAt;

    public Producto() {
    }

    private Producto(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.slug = builder.slug;
        this.description = builder.description;
        this.sku = builder.sku;
        this.status = builder.status;
        this.categories = builder.categories;
        this.price = builder.price;
        this.ranking = builder.ranking;
        this.recentComments = builder.recentComments;
        this.hasMoreComments = builder.hasMoreComments;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setId(Long id) {
        this.id = id == null ? null : id.toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public Price getPrice() {
        return price;
    }

    public void setPrice(Price price) {
        this.price = price;
    }

    public Ranking getRanking() {
        return ranking;
    }

    public void setRanking(Ranking ranking) {
        this.ranking = ranking;
    }

    public List<Comment> getRecentComments() {
        return recentComments;
    }

    public void setRecentComments(List<Comment> recentComments) {
        this.recentComments = recentComments;
    }

    public Boolean getHasMoreComments() {
        return hasMoreComments;
    }

    public void setHasMoreComments(Boolean hasMoreComments) {
        this.hasMoreComments = hasMoreComments;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Legacy aliases kept temporarily to avoid breaking older request/service code.
    public String getNombre() {
        return getName();
    }

    public void setNombre(String nombre) {
        setName(nombre);
    }

    public String getDescripcion() {
        return getDescription();
    }

    public void setDescripcion(String descripcion) {
        setDescription(descripcion);
    }

    public BigDecimal getPrecio() {
        return price == null ? null : price.getCurrent();
    }

    public void setPrecio(BigDecimal precio) {
        if (precio == null) {
            this.price = null;
            return;
        }
        if (this.price == null) {
            this.price = Price.builder().current(precio).build();
            return;
        }
        this.price.setCurrent(precio);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Producto producto = (Producto) o;
        return Objects.equals(id, producto.id)
            && Objects.equals(name, producto.name)
            && Objects.equals(slug, producto.slug)
            && Objects.equals(description, producto.description)
            && Objects.equals(sku, producto.sku)
            && Objects.equals(status, producto.status)
            && Objects.equals(categories, producto.categories)
            && Objects.equals(price, producto.price)
            && Objects.equals(ranking, producto.ranking)
            && Objects.equals(recentComments, producto.recentComments)
            && Objects.equals(hasMoreComments, producto.hasMoreComments)
            && Objects.equals(createdAt, producto.createdAt)
            && Objects.equals(updatedAt, producto.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, slug, description, sku, status, categories, price, ranking,
            recentComments, hasMoreComments, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "Producto{" +
                "id=" + id +
            ", name='" + name + '\'' +
            ", slug='" + slug + '\'' +
            ", description='" + description + '\'' +
            ", sku='" + sku + '\'' +
            ", status='" + status + '\'' +
            ", categories=" + categories +
            ", price=" + price +
            ", ranking=" + ranking +
            ", recentComments=" + recentComments +
            ", hasMoreComments=" + hasMoreComments +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
                '}';
    }

    public static class Builder {

        private String id;
        private String name;
        private String slug;
        private String description;
        private String sku;
        private String status;
        private List<Category> categories;
        private Price price;
        private Ranking ranking;
        private List<Comment> recentComments;
        private Boolean hasMoreComments;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder id(Long id) {
            this.id = id == null ? null : id.toString();
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder slug(String slug) {
            this.slug = slug;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder sku(String sku) {
            this.sku = sku;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder categories(List<Category> categories) {
            this.categories = categories;
            return this;
        }

        public Builder price(Price price) {
            this.price = price;
            return this;
        }

        public Builder ranking(Ranking ranking) {
            this.ranking = ranking;
            return this;
        }

        public Builder recentComments(List<Comment> recentComments) {
            this.recentComments = recentComments;
            return this;
        }

        public Builder hasMoreComments(Boolean hasMoreComments) {
            this.hasMoreComments = hasMoreComments;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        // Legacy aliases kept temporarily to avoid breaking older request/service code.
        public Builder nombre(String nombre) {
            this.name = nombre;
            return this;
        }

        public Builder descripcion(String descripcion) {
            this.description = descripcion;
            return this;
        }

        public Builder precio(BigDecimal precio) {
            this.price = precio == null ? null : Price.builder().current(precio).build();
            return this;
        }

        public Builder reviews(Review reviews) {
            if (reviews == null) {
                return this;
            }
            this.recentComments = List.of(Comment.builder()
                    .username(reviews.getAutor())
                    .body(reviews.getReviewText())
                    .build());
            return this;
        }

        public Producto build() {
            return new Producto(this);
        }
    }
}

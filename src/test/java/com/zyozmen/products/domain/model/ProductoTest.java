package com.zyozmen.products.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductoTest {

    @Test
    void builderShouldPopulateFieldsAndLegacyAliases() {
        Instant createdAt = Instant.parse("2025-11-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-06-17T13:00:00Z");

        Producto producto = Producto.builder()
                .id("1")
                .name("Wireless Headphones")
                .slug("wireless-headphones")
                .description("Great sound")
                .sku("SKU-001")
                .status("active")
                .categories(List.of(Category.builder().categoryId(1L).name("Audio").build()))
                .price(Price.builder().current(new BigDecimal("199.99")).build())
                .ranking(Ranking.builder().build())
                .recentComments(List.of(Comment.builder().commentId("comment-1").build()))
                .hasMoreComments(true)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        assertThat(producto.getId()).isEqualTo("1");
        assertThat(producto.getName()).isEqualTo("Wireless Headphones");
        assertThat(producto.getSlug()).isEqualTo("wireless-headphones");
        assertThat(producto.getDescription()).isEqualTo("Great sound");
        assertThat(producto.getSku()).isEqualTo("SKU-001");
        assertThat(producto.getStatus()).isEqualTo("active");
        assertThat(producto.getCategories()).hasSize(1);
        assertThat(producto.getPrice().getCurrent()).isEqualByComparingTo("199.99");
        assertThat(producto.getRanking()).isNotNull();
        assertThat(producto.getRecentComments()).hasSize(1);
        assertThat(producto.getHasMoreComments()).isTrue();
        assertThat(producto.getCreatedAt()).isEqualTo(createdAt);
        assertThat(producto.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(producto.getNombre()).isEqualTo("Wireless Headphones");
        assertThat(producto.getPrecio()).isEqualByComparingTo("199.99");
    }

    @Test
    void settersAndLegacyMethodsShouldUpdateState() {
        Producto producto = new Producto();
        Instant createdAt = Instant.parse("2025-11-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-06-17T13:00:00Z");

        producto.setId("10");
        producto.setId(11L);
        producto.setName("Updated Name");
        producto.setSlug("updated-slug");
        producto.setDescription("Updated description");
        producto.setSku("SKU-002");
        producto.setStatus("inactive");
        producto.setCategories(List.of(Category.builder().categoryId(2L).name("Gaming").build()));
        producto.setPrice(Price.builder().current(new BigDecimal("99.99")).build());
        producto.setRanking(Ranking.builder().averageRating(new BigDecimal("4.2")).build());
        producto.setRecentComments(List.of(Comment.builder().commentId("comment-2").build()));
        producto.setHasMoreComments(false);
        producto.setCreatedAt(createdAt);
        producto.setUpdatedAt(updatedAt);

        producto.setNombre("Legacy Name");
        producto.setDescripcion("Legacy description");
        producto.setPrecio(new BigDecimal("55.50"));
        assertThat(producto.getName()).isEqualTo("Legacy Name");
        assertThat(producto.getDescription()).isEqualTo("Legacy description");
        assertThat(producto.getPrecio()).isEqualByComparingTo("55.50");

        producto.setPrecio(null);
        assertThat(producto.getPrecio()).isNull();

        producto.setPrecio(new BigDecimal("12.34"));
        assertThat(producto.getPrecio()).isEqualByComparingTo("12.34");

        assertThat(producto.getId()).isEqualTo("11");
        assertThat(producto.getSlug()).isEqualTo("updated-slug");
        assertThat(producto.getSku()).isEqualTo("SKU-002");
        assertThat(producto.getStatus()).isEqualTo("inactive");
        assertThat(producto.getCategories()).hasSize(1);
        assertThat(producto.getPrice().getCurrent()).isEqualByComparingTo("12.34");
        assertThat(producto.getRanking()).isNotNull();
        assertThat(producto.getRecentComments()).hasSize(1);
        assertThat(producto.getHasMoreComments()).isFalse();
        assertThat(producto.getCreatedAt()).isEqualTo(createdAt);
        assertThat(producto.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void builderShouldSupportReviewsAndEqualityBehavior() {
        Review review = Review.builder()
                .autor("Jane")
                .reviewText("Excellent")
                .build();

        Producto producto = Producto.builder()
                .id(7L)
                .nombre("Alias Name")
                .descripcion("Alias description")
                .precio(new BigDecimal("7.77"))
                .reviews(review)
                .build();

        Producto sameProducto = Producto.builder()
                .id(7L)
                .nombre("Alias Name")
                .descripcion("Alias description")
                .precio(new BigDecimal("7.77"))
                .reviews(review)
                .build();

        Producto differentProducto = Producto.builder()
                .id(8L)
                .nombre("Other")
                .build();

        Producto productoWithNullReview = Producto.builder().reviews(null).build();

        assertThat(producto.getName()).isEqualTo("Alias Name");
        assertThat(producto.getDescription()).isEqualTo("Alias description");
        assertThat(producto.getPrecio()).isEqualByComparingTo("7.77");
        assertThat(producto.getRecentComments()).hasSize(1);
        assertThat(producto.getRecentComments().get(0).getUsername()).isEqualTo("Jane");
        assertThat(producto.getRecentComments().get(0).getBody()).isEqualTo("Excellent");
        assertThat(productoWithNullReview.getRecentComments()).isNull();

        assertThat(producto).isEqualTo(producto);
        assertThat(producto).isEqualTo(sameProducto);
        assertThat(producto.hashCode()).isEqualTo(sameProducto.hashCode());
        assertThat(producto).isNotEqualTo(null);
        assertThat(producto).isNotEqualTo("not a product");
        assertThat(producto).isNotEqualTo(differentProducto);
        assertThat(producto.toString()).contains("Alias Name");
    }
}

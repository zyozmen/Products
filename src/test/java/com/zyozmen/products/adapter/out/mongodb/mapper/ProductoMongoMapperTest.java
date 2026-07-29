package com.zyozmen.products.adapter.out.mongodb.mapper;

import com.zyozmen.products.adapter.out.mongodb.document.CategoryDocument;
import com.zyozmen.products.adapter.out.mongodb.document.CommentDocument;
import com.zyozmen.products.adapter.out.mongodb.document.PriceDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProductoMongoDocument;
import com.zyozmen.products.adapter.out.mongodb.document.RankingDocument;
import com.zyozmen.products.adapter.out.mongodb.document.RatingDistributionDocument;
import com.zyozmen.products.domain.model.Category;
import com.zyozmen.products.domain.model.Comment;
import com.zyozmen.products.domain.model.Price;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.model.Ranking;
import com.zyozmen.products.domain.model.RatingDistribution;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductoMongoMapperTest {

    private final ProductoMongoMapper mapper = new ProductoMongoMapper(null);

    @Test
    void toDomainShouldMapDocumentToDomainModel() {
        ProductoMongoDocument document = ProductoMongoDocument.builder()
                .id("mongo-id")
                .name("Headphones")
                .slug("headphones")
                .description("Great audio")
                .sku("SKU-1")
                .status("active")
                .categories(List.of(CategoryDocument.builder().categoryId(1L).name("Audio").slug("audio").build()))
                .price(PriceDocument.builder().current(BigDecimal.valueOf(199.99)).original(BigDecimal.valueOf(249.99)).currency("USD").build())
                .ranking(RankingDocument.builder()
                        .averageRating(BigDecimal.valueOf(4.5))
                        .totalReviews(120)
                        .ratingDistribution(RatingDistributionDocument.builder().fiveStar(90).fourStar(20).threeStar(10).twoStar(0).oneStar(0).build())
                        .build())
                .recentComments(List.of(CommentDocument.builder().username("alice").body("Nice").build()))
                .hasMoreComments(true)
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2024-01-02T00:00:00Z"))
                .build();

        Producto producto = mapper.toDomain(document);

        assertThat(producto.getId()).isEqualTo("mongo-id");
        assertThat(producto.getName()).isEqualTo("Headphones");
        assertThat(producto.getPrice().getCurrent()).isEqualByComparingTo("199.99");
        assertThat(producto.getRanking().getAverageRating()).isEqualByComparingTo("4.5");
        assertThat(producto.getRecentComments()).hasSize(1);
        assertThat(producto.getHasMoreComments()).isTrue();
    }

    @Test
    void toDocumentShouldMapDomainModelToDocument() {
        Producto producto = Producto.builder()
                .id("domain-id")
                .name("Keyboard")
                .slug("keyboard")
                .description("Mechanical")
                .sku("SKU-2")
                .status("active")
                .categories(List.of(Category.builder().categoryId(2L).name("Peripherals").slug("peripherals").build()))
                .price(Price.builder().current(BigDecimal.valueOf(89.99)).original(BigDecimal.valueOf(99.99)).currency("USD").build())
                .ranking(Ranking.builder().averageRating(BigDecimal.valueOf(4.2)).totalReviews(50).ratingDistribution(RatingDistribution.builder().fiveStar(40).fourStar(10).threeStar(0).twoStar(0).oneStar(0).build()).build())
                .recentComments(List.of(Comment.builder().username("bob").body("Good").build()))
                .hasMoreComments(false)
                .createdAt(Instant.parse("2024-02-01T00:00:00Z"))
                .updatedAt(Instant.parse("2024-02-02T00:00:00Z"))
                .build();

        ProductoMongoDocument document = mapper.toDocument(producto);

        assertThat(document.getId()).isEqualTo("domain-id");
        assertThat(document.getName()).isEqualTo("Keyboard");
        assertThat(document.getPrice().getCurrent()).isEqualByComparingTo("89.99");
        assertThat(document.getRanking().getAverageRating()).isEqualByComparingTo("4.2");
        assertThat(document.getRecentComments()).hasSize(1);
        assertThat(document.getHasMoreComments()).isFalse();
    }
}

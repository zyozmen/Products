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
import org.springframework.stereotype.Component;
import org.springframework.web.method.support.CompositeUriComponentsContributor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Mapper de persistencia MongoDB.
 * Convierte entre el modelo de dominio puro y el documento MongoDB de infraestructura.
 */
@Component
public class ProductoMongoMapper {

    public ProductoMongoMapper(CompositeUriComponentsContributor compositeUriComponentsContributor) {
    }

    public Producto toDomain(ProductoMongoDocument document) {
        return Producto.builder()
                .id(document.getId())
                .name(document.getName())
                .slug(document.getSlug())
                .description(document.getDescription())
                .sku(document.getSku())
                .status(document.getStatus())
                .categories(toCategoryDomainList(document.getCategories()))
                .price(toPriceDomain(document.getPrice()))
                .ranking(toRankingDomain(document.getRanking()))
                .recentComments(toCommentDomainList(document.getRecentComments()))
                .hasMoreComments(document.getHasMoreComments())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    public ProductoMongoDocument toDocument(Producto domain) {
        return ProductoMongoDocument.builder()
                .id(domain.getId())
                .name(domain.getName())
                .slug(domain.getSlug())
                .description(domain.getDescription())
                .sku(domain.getSku())
                .status(domain.getStatus())
                .categories(toCategoryDocumentList(domain.getCategories()))
                .price(toPriceDocument(domain.getPrice()))
                .ranking(toRankingDocument(domain.getRanking()))
                .recentComments(toCommentDocumentList(domain.getRecentComments()))
                .hasMoreComments(domain.getHasMoreComments())
                .createdAt(domain.getCreatedAt()== null ? Instant.now(): domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt() == null ? Instant.now(): domain.getUpdatedAt())
                .build();
    }

    public ProductoMongoDocument toDocument(Producto domain, String existingMongoId) {
        return ProductoMongoDocument.builder()
                .id(existingMongoId)
                .name(domain.getName())
                .slug(domain.getSlug())
                .description(domain.getDescription())
                .sku(domain.getSku())
                .status(domain.getStatus())
                .categories(toCategoryDocumentList(domain.getCategories()))
                .price(toPriceDocument(domain.getPrice()))
                .ranking(toRankingDocument(domain.getRanking()))
                .recentComments(toCommentDocumentList(domain.getRecentComments()))
                .hasMoreComments(domain.getHasMoreComments())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private Price toPriceDomain(PriceDocument doc) {
        if (doc == null) return null;
        return Price.builder()
                .current(doc.getCurrent())
                .original(doc.getOriginal())
                .currency(doc.getCurrency())
                .discountPercentage(doc.getDiscountPercentage())
                .taxInclusive(doc.getTaxInclusive())
                .build();
    }

    private PriceDocument toPriceDocument(Price domain) {
        if (domain == null) return null;
        return PriceDocument.builder()
                .current(domain.getCurrent())
                .original(domain.getOriginal())
                .currency(domain.getCurrency())
                .discountPercentage(domain.getDiscountPercentage())
                .taxInclusive(domain.getTaxInclusive())
                .build();
    }

    private Ranking toRankingDomain(RankingDocument doc) {
        if (doc == null) return
                Ranking.builder()
                        .averageRating(BigDecimal.ZERO)
                        .totalReviews(0)
                        .ratingDistribution(toRatingDistributionDomain(null))
                        .build();
        return Ranking.builder()
                .averageRating(doc.getAverageRating())
                .totalReviews(doc.getTotalReviews())
                .ratingDistribution(toRatingDistributionDomain(doc.getRatingDistribution()))
                .build();
    }

    private RankingDocument toRankingDocument(Ranking domain) {
        if (domain == null) return null;
        return RankingDocument.builder()
                .averageRating(domain.getAverageRating())
                .totalReviews(domain.getTotalReviews())
                .ratingDistribution(toRatingDistributionDocument(domain.getRatingDistribution()))
                .build();
    }

    private RatingDistribution toRatingDistributionDomain(RatingDistributionDocument doc) {
        if (doc == null)

            return RatingDistribution.builder()
                    .oneStar(0)
                    .twoStar(0)
                    .threeStar(0)
                    .fourStar(0)
                    .fiveStar(0)
                    .build();
        return RatingDistribution.builder()
                .fiveStar(doc.getFiveStar())
                .fourStar(doc.getFourStar())
                .threeStar(doc.getThreeStar())
                .twoStar(doc.getTwoStar())
                .oneStar(doc.getOneStar())
                .build();
    }

    private RatingDistributionDocument toRatingDistributionDocument(RatingDistribution domain) {
        if (domain == null) return null;
        return RatingDistributionDocument.builder()
                .fiveStar(domain.getFiveStar())
                .fourStar(domain.getFourStar())
                .threeStar(domain.getThreeStar())
                .twoStar(domain.getTwoStar())
                .oneStar(domain.getOneStar())
                .build();
    }

    private List<Category> toCategoryDomainList(List<CategoryDocument> docs) {
        if (docs == null) return List.of();
        return docs.stream().map(this::toCategoryDomain).toList();
    }

    private List<CategoryDocument> toCategoryDocumentList(List<Category> categories) {
        if (categories == null) return List.of();
        return categories.stream().map(this::toCategoryDocument).toList();
    }

    public Category toCategoryDomain(CategoryDocument doc) {
        if (doc == null) return null;
        return Category.builder()
                .categoryId(doc.getCategoryId())
                .name(doc.getName())
                .slug(doc.getSlug())
                .productsCount(doc.getProductsCount())
                .build();
    }

    public CategoryDocument toCategoryDocument(Category category) {
        if (category == null) return null;
        return CategoryDocument.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .slug(category.getSlug())
                .build();
    }

    private List<Comment> toCommentDomainList(List<CommentDocument> docs) {
        if (docs == null) return List.of();
        return docs.stream().map(this::toCommentDomain).toList();
    }

    private List<CommentDocument> toCommentDocumentList(List<Comment> comments) {
        if (comments == null) return List.of();
        return comments.stream().map(this::toCommentDocument).toList();
    }

    private Comment toCommentDomain(CommentDocument doc) {
        if (doc == null) return null;
        return Comment.builder()
                .commentId(doc.getCommentId())
                .userId(doc.getUserId())
                .username(doc.getUsername())
                .rating(doc.getRating())
                .title(doc.getTitle())
                .body(doc.getBody())
                .createdAt(doc.getCreatedAt())
                .build();
    }

    private CommentDocument toCommentDocument(Comment comment) {
        if (comment == null) return null;
        return CommentDocument.builder()
                .commentId(comment.getCommentId())
                .userId(comment.getUserId())
                .username(comment.getUsername())
                .rating(comment.getRating())
                .title(comment.getTitle())
                .body(comment.getBody())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}

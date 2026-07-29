package com.zyozmen.products.adapter.in.web.mapper;
import java.util.ArrayList;
import com.zyozmen.products.adapter.in.web.dto.CategoryDTO;
import com.zyozmen.products.adapter.in.web.dto.RankingDTO;
import com.zyozmen.products.adapter.in.web.dto.RatingDistributionDTO;
import com.zyozmen.products.adapter.in.web.dto.RecentCommentDTO;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.model.RatingDistribution;
import com.zyozmen.products.domain.model.Ranking;
import com.zyozmen.products.domain.model.Comment;
import com.zyozmen.products.domain.model.Category;
import com.zyozmen.products.domain.model.Price;
import com.zyozmen.products.adapter.in.web.dto.PriceDTO;
import com.zyozmen.products.adapter.in.web.dto.ProductoListItemDTO;
import com.zyozmen.products.adapter.in.web.dto.ProductoRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.ProductoResponseDTO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper del adaptador web. Convierte entre los DTOs HTTP y el modelo de dominio.
 * El controlador no conoce el modelo de dominio directamente en las firmas
 * públicas de la API; esta clase hace el puente.
 */
@Component
public class ProductoWebMapper {

    public Producto toDomain(ProductoRequestDTO dto) {
        return Producto.builder()
                .id(dto.getId())
                .name(dto.getName())
                .slug(dto.getSlug())
                .description(dto.getDescription())
                .sku(dto.getSku())
                .status(dto.getStatus())
                .categories(toCategoryDomainList(dto.getCategories()))
                .price(toPriceDomain(dto.getPrice()))
                .ranking(toRankingDomain(dto.getRanking()))
                .recentComments(toCommentDomainList(dto.getRecentComments()))
                .hasMoreComments(dto.getHasMoreComments())
                .build();
    }

    public ProductoResponseDTO toResponseDTO(Producto producto) {
        return ProductoResponseDTO.builder()
                .id(producto.getId())
                .name(producto.getName())
                .slug(producto.getSlug())
                .description(producto.getDescription())
                .sku(producto.getSku())
                .status(producto.getStatus())
                .categories(toCategoryDTOList(producto.getCategories()))
                .price(toPriceDTO(producto.getPrice()))
                .ranking(toRankingDTO(producto.getRanking()))
                .recentComments(toRecentCommentDTOList(producto.getRecentComments()))
                .createdAt(producto.getCreatedAt())
                .updatedAt(producto.getUpdatedAt())
                .build();
    }

    public ProductoListItemDTO toListItemDTO(Producto producto) {
        if (producto == null) return null;
        return ProductoListItemDTO.builder()
                .id(producto.getId())
                .name(producto.getName())
                .categoryIds(toCategoryIdList(producto.getCategories()))
                .currentPrice(producto.getPrice() == null ? null : producto.getPrice().getCurrent())
                .originalPrice(producto.getPrice() == null ? null : producto.getPrice().getOriginal())
                .priceCurrency(producto.getPrice() == null ? null : producto.getPrice().getCurrency())
                .averageRating(producto.getRanking() == null ? null : producto.getRanking().getAverageRating())
                .totalReviews(producto.getRanking() == null ? null : producto.getRanking().getTotalReviews())
                .build();
    }

    private List<Long> toCategoryIdList(List<Category> categories) {
        if (categories == null) return List.of();
        return categories.stream().map(Category::getCategoryId).toList();
    }

    private Price toPriceDomain(PriceDTO dto) {
        if (dto == null) return null;
        return Price.builder()
                .current(dto.getCurrent())
                .original(dto.getOriginal())
                .currency(dto.getCurrency())
                .discountPercentage(dto.getDiscountPercentage())
                .taxInclusive(dto.getTaxInclusive())
                .build();
    }

    private Ranking toRankingDomain(RankingDTO dto) {
        if (dto == null) return null;
        return Ranking.builder()
                .averageRating(dto.getAverageRating())
                .totalReviews(dto.getTotalReviews())
                .ratingDistribution(toRatingDistributionDomain(dto.getRatingDistribution()))
                .build();
    }

    private RatingDistribution toRatingDistributionDomain(RatingDistributionDTO dto) {
        if (dto == null) return null;
        return RatingDistribution.builder()
                .fiveStar(dto.getFiveStar())
                .fourStar(dto.getFourStar())
                .threeStar(dto.getThreeStar())
                .twoStar(dto.getTwoStar())
                .oneStar(dto.getOneStar())
                .build();
    }

    private PriceDTO toPriceDTO(Price price) {
        if (price == null) return null;
        return PriceDTO.builder()
                .current(price.getCurrent())
                .original(price.getOriginal())
                .currency(price.getCurrency())
                .discountPercentage(price.getDiscountPercentage())
                .taxInclusive(price.getTaxInclusive())
                .build();
    }

    private RankingDTO toRankingDTO(Ranking ranking) {
        if (ranking == null) return null;
        return RankingDTO.builder()
                .averageRating(ranking.getAverageRating())
                .totalReviews(ranking.getTotalReviews())
                .ratingDistribution(toRatingDistributionDTO(ranking.getRatingDistribution()))
                .build();
    }

    private RatingDistributionDTO toRatingDistributionDTO(RatingDistribution distribution) {
        if (distribution == null) return null;
        return RatingDistributionDTO.builder()
                .fiveStar(distribution.getFiveStar())
                .fourStar(distribution.getFourStar())
                .threeStar(distribution.getThreeStar())
                .twoStar(distribution.getTwoStar())
                .oneStar(distribution.getOneStar())
                .build();
    }

    public List<CategoryDTO> toCategoryDTOList(List<Category> categories) {
        if (categories == null) return List.of();
        return categories.stream().map(this::toCategoryDTO).toList();
    }

    private List<Category> toCategoryDomainList(List<CategoryDTO> categories) {
        if (categories == null) return List.of();
        return categories.stream().map(this::toCategoryDomain).toList();
    }

    private Category toCategoryDomain(CategoryDTO dto) {
        if (dto == null) return null;
        return Category.builder()
                .categoryId(dto.getCategoryId())
                .name(dto.getName())
                .slug(dto.getSlug())
                .productsCount(dto.getProductsCount())
                .build();
    }

    public CategoryDTO toCategoryDTO(Category category) {
        if (category == null) return null;
        return CategoryDTO.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .slug(category.getSlug())
                .productsCount(category.getProductsCount())
                .build();
    }

    private List<RecentCommentDTO> toRecentCommentDTOList(List<Comment> comments) {
        if (comments == null) return List.of();
        return comments.stream().map(this::toRecentCommentDTO).toList();
    }

    private List<Comment> toCommentDomainList(List<RecentCommentDTO> comments) {
        if (comments == null) return List.of();
        return comments.stream().map(this::toCommentDomain).toList();
    }

    private Comment toCommentDomain(RecentCommentDTO dto) {
        if (dto == null) return null;
        return Comment.builder()
                .commentId(dto.getCommentId())
                .userId(dto.getUserId())
                .username(dto.getUsername())
                .rating(dto.getRating())
                .title(dto.getTitle())
                .body(dto.getBody())
                .createdAt(dto.getCreatedAt())
                .build();
    }

    private RecentCommentDTO toRecentCommentDTO(Comment comment) {
        if (comment == null) return null;
        return RecentCommentDTO.builder()
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

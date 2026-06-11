package com.zyozmen.products.adapter.out.mongodb.mapper;

import com.zyozmen.products.adapter.out.mongodb.document.ProductoMongoDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ReviewDocument;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.model.Review;
import org.springframework.stereotype.Component;

/**
 * Mapper de persistencia MongoDB.
 * Convierte entre el modelo de dominio puro y el documento MongoDB de infraestructura.
 * El campo sequenceId del documento corresponde al id numérico del dominio.
 */
@Component
public class ProductoMongoMapper {

    public Producto toDomain(ProductoMongoDocument document) {
        return Producto.builder()
                .id(document.getSequenceId())
                .nombre(document.getNombre())
                .descripcion(document.getDescripcion())
                .precio(document.getPrecio())
                .reviews(toReviewDomain(document.getReviews()))
                .build();
    }

    public ProductoMongoDocument toDocument(Producto domain) {
        return ProductoMongoDocument.builder()
                .sequenceId(domain.getId())
                .nombre(domain.getNombre())
                .descripcion(domain.getDescripcion())
                .precio(domain.getPrecio())
                .reviews(toReviewDocument(domain.getReviews()))
                .build();
    }

    public ProductoMongoDocument toDocument(Producto domain, String existingMongoId) {
        return ProductoMongoDocument.builder()
                .id(existingMongoId)
                .sequenceId(domain.getId())
                .nombre(domain.getNombre())
                .descripcion(domain.getDescripcion())
                .precio(domain.getPrecio())
                .reviews(toReviewDocument(domain.getReviews()))
                .build();
    }

    private Review toReviewDomain(ReviewDocument doc) {
        if (doc == null) return null;
        return Review.builder()
                .autor(doc.getAutor())
                .stars(doc.getStars())
                .review(doc.getReview())
                .email(doc.getEmail())
                .build();
    }

    private ReviewDocument toReviewDocument(Review review) {
        if (review == null) return null;
        return ReviewDocument.builder()
                .autor(review.getAutor())
                .stars(review.getStars())
                .review(review.getReview())
                .email(review.getEmail())
                .build();
    }
}

package com.zyozmen.products.adapter.out.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;

/**
 * Documento embebido que representa una reseña de un producto.
 * Se almacena anidado dentro de ProductoMongoDocument.
 * El campo stars usa BigDecimal para preservar la precisión decimal
 * (equivalente a $numberDecimal en BSON).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDocument {

    @Field("autor")
    private String autor;

    @Field("stars")
    private BigDecimal stars;

    @Field("review")
    private String review;

    @Field("email")
    private String email;
}

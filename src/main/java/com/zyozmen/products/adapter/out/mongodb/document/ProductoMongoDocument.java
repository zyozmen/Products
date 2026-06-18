package com.zyozmen.products.adapter.out.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/**
 * Documento MongoDB de infraestructura.
 * Las anotaciones de persistencia residen aquí, no en el modelo de dominio,
 * manteniendo el dominio libre de dependencias de frameworks.
 *
 * Se usa String como @Id para aprovechar el ObjectId nativo de MongoDB,
 * mientras que el resto de atributos reflejan la estructura del documento
 * enriquecido de catálogo (precio, ranking y comentarios recientes).
 */
@Document(collection = "Products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoMongoDocument {

    @Id
    private String id;

    @Field("name")
    private String name;

    @Indexed(unique = true)
    @Field("slug")
    private String slug;

    @Field("description")
    private String description;

    @Indexed(unique = true)
    @Field("sku")
    private String sku;

    @Field("status")
    private String status;

    @Field("categories")
    private List<CategoryDocument> categories;

    @Field("price")
    private PriceDocument price;

    @Field("ranking")
    private RankingDocument ranking;

    @Field("recent_comments")
    private List<CommentDocument> recentComments;

    @Field("has_more_comments")
    private Boolean hasMoreComments;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;
}

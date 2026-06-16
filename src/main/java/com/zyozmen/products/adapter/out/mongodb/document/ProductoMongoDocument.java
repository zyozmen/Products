package com.zyozmen.products.adapter.out.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;

/**
 * Documento MongoDB de infraestructura.
 * Las anotaciones de persistencia residen aquí, no en el modelo de dominio,
 * manteniendo el dominio libre de dependencias de frameworks.
 *
 * Se usa String como @Id para aprovechar el ObjectId nativo de MongoDB.
 * El campo sequenceId almacena el identificador numérico del dominio y
 * está indexado como único para soportar findById(Long) eficientemente.
 *
 * El campo reviews es un documento embebido con autor, stars ($numberDecimal),
 * review y email, almacenado directamente en el mismo documento de producto.
 */
@Document(collection = "Products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoMongoDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("sequence_id")
    private Long sequenceId;

    @Field("nombre")
    private String nombre;

    @Field("descripcion")
    private String descripcion;

    @Field("precio")
    private BigDecimal precio;

    @Field("reviews")
    private ReviewDocument reviews;
}

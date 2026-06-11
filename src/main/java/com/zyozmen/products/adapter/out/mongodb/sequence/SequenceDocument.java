package com.zyozmen.products.adapter.out.mongodb.sequence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Documento de contador para la generación de IDs numéricos auto-incrementales.
 * Cada secuencia está identificada por un nombre único (e.g. "productos_sequence").
 */
@Document(collection = "sequences")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SequenceDocument {

    @Id
    private String id;

    private long seq;
}

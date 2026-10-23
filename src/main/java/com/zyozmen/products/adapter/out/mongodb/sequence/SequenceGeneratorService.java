package com.zyozmen.products.adapter.out.mongodb.sequence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * Servicio de generación de IDs auto-incrementales para MongoDB.
 *
 * Usa findAndModify de forma atómica sobre la colección "sequences" para
 * incrementar el contador y devolver el nuevo valor, evitando condiciones
 * de carrera en entornos concurrentes.
 */
@Service
@RequiredArgsConstructor
public class SequenceGeneratorService {

    public static final String PRODUCTO_SEQUENCE = "productos_sequence";
    public static final String ORDER_SEQUENCE = "orders_sequence";
    public static final String CATEGORY_SEQUENCE = "categories_sequence";
    public static final String LOTE_SEQUENCE = "lotes_sequence";
    public static final String CONTENEDOR_SEQUENCE = "contenedores_sequence";

    private final MongoOperations mongoOperations;

    /**
     * Genera y devuelve el siguiente Long ID para la secuencia indicada.
     *
     * @param sequenceName nombre de la secuencia (usar constante {@link #PRODUCTO_SEQUENCE})
     * @return el próximo valor de la secuencia
     */
    public long nextSequence(String sequenceName) {
        Query query = new Query(Criteria.where("_id").is(sequenceName));
        Update update = new Update().inc("seq", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options()
                .returnNew(true)
                .upsert(true);

        SequenceDocument counter = mongoOperations.findAndModify(
                query, update, options, SequenceDocument.class);

        if (counter == null) {
            throw new IllegalStateException("No se pudo generar el siguiente ID para " + sequenceName);
        }
        return counter.getSeq();
    }
}

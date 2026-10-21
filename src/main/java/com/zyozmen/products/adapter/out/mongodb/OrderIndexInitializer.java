package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.OrderMongoDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import com.zyozmen.products.domain.model.OrderStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderIndexInitializer implements ApplicationRunner {
    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        var indexes = mongoTemplate.indexOps(OrderMongoDocument.class);
        indexes.ensureIndex(new Index()
                .on("username", Sort.Direction.ASC)
                .on("created_at", Sort.Direction.DESC)
                .named("username_created_at_idx"));
        indexes.ensureIndex(new Index().on("status", Sort.Direction.ASC).named("status_idx"));
        indexes.ensureIndex(new Index().on("created_at", Sort.Direction.ASC).named("created_at_idx"));
        indexes.ensureIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .on("created_at", Sort.Direction.ASC)
                .named("status_created_at_idx"));
        indexes.ensureIndex(new Index().on("pqr", Sort.Direction.ASC).named("pqr_idx"));

        // Migración: el estado histórico DESPACHADA ya no existe; pasa a EMPACADA (orden e historial).
        mongoTemplate.updateMulti(
                new Query(Criteria.where("status").is("DESPACHADA")),
                new Update().set("status", OrderStatus.EMPACADA.name()),
                OrderMongoDocument.class);
        for (String field : new String[] {"from_status", "to_status"}) {
            mongoTemplate.updateMulti(
                    new Query(Criteria.where("status_history." + field).is("DESPACHADA")),
                    new Update().set("status_history.$[entry]." + field, OrderStatus.EMPACADA.name())
                            .filterArray(Criteria.where("entry." + field).is("DESPACHADA")),
                    OrderMongoDocument.class);
        }
    }
}

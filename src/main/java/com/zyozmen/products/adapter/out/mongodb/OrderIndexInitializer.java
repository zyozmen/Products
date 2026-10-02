package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.OrderMongoDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
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
    }
}

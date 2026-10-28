package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.RevokedTokenDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Crea el índice TTL que purga los tokens revocados una vez expirados.
 */
@Component
@RequiredArgsConstructor
public class RevokedTokenIndexInitializer implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        mongoTemplate.indexOps(RevokedTokenDocument.class).ensureIndex(new Index()
                .on("expires_at", Sort.Direction.ASC)
                .expire(0, TimeUnit.SECONDS)
                .named("expires_at_ttl_idx"));
    }
}

package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.ProviderAccumulatorsDocument;
import com.zyozmen.products.adapter.out.mongodb.document.DailySettlementSettingsDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProviderDailyMovementsDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class DailySettlementIndexInitializer implements ApplicationRunner {
    private final MongoTemplate template;

    @Override
    public void run(ApplicationArguments args) {
        var indexes = template.indexOps(ProviderAccumulatorsDocument.class);
        indexes.ensureIndex(new Index().on("providerId", Sort.Direction.ASC)
                .on("date", Sort.Direction.ASC).unique());
        indexes.ensureIndex(new Index().on("date", Sort.Direction.ASC).on("providerId", Sort.Direction.ASC));
        template.indexOps(ProviderDailyMovementsDocument.class).ensureIndex(new Index()
                .on("orderId", Sort.Direction.ASC).on("itemIndex", Sort.Direction.ASC)
                .on("movementType", Sort.Direction.ASC).unique());
        template.upsert(Query.query(Criteria.where("id").is(DailySettlementSettingsDocument.ID)),
                new Update().setOnInsert("activatedAt", null), DailySettlementSettingsDocument.class);
        template.updateFirst(Query.query(Criteria.where("id").is(DailySettlementSettingsDocument.ID)
                        .and("activatedAt").is(null)),
                Update.update("activatedAt", Instant.now()), DailySettlementSettingsDocument.class);
    }
}

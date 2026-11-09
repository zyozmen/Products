package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.ReferralDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReferralIndexInitializer implements ApplicationRunner {
    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        var indexes = mongoTemplate.indexOps(ReferralDocument.class);
        indexes.ensureIndex(new Index().on("idReferido", Sort.Direction.ASC)
                .unique().named("referral_referred_unique"));
        indexes.ensureIndex(new Index().on("IdReferente", Sort.Direction.ASC)
                .on("created_at", Sort.Direction.DESC).on("_id", Sort.Direction.DESC)
                .named("referral_referrer_created_idx"));
    }
}

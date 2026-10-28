package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.ProductMarketplaceDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProductDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketplaceIndexInitializer implements ApplicationRunner {
    private final MongoTemplate template;

    @Override
    public void run(ApplicationArguments args) {
        var indexes = template.indexOps(ProductMarketplaceDocument.class);
        indexes.ensureIndex(new Index().on("presentacionId", Sort.Direction.ASC).unique());
        indexes.ensureIndex(new Index().on("sku", Sort.Direction.ASC).unique());
        indexes.ensureIndex(new Index().on("estado", Sort.Direction.ASC)
                .on("stockDisponible", Sort.Direction.ASC).on("cepaId", Sort.Direction.ASC));
        template.indexOps(ProductDocument.class)
                .ensureIndex(new Index().on("nombreNormalizado", Sort.Direction.ASC).unique());
    }
}

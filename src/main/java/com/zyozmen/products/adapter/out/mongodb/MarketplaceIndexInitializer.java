package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.ProductoMarketplaceDocument;
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
        var indexes = template.indexOps(ProductoMarketplaceDocument.class);
        indexes.ensureIndex(new Index().on("presentacionId", Sort.Direction.ASC).unique());
        indexes.ensureIndex(new Index().on("sku", Sort.Direction.ASC).unique());
    }
}

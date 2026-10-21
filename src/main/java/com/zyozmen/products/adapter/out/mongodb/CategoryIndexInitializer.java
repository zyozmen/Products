package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.CatalogCategoryDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProductoMongoDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryIndexInitializer implements ApplicationRunner {
    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        mongoTemplate.indexOps(CatalogCategoryDocument.class)
                .ensureIndex(new Index().on("name", Sort.Direction.ASC).unique().named("category_name_unique"));
        mongoTemplate.indexOps(ProductoMongoDocument.class)
                .ensureIndex(new Index().on("categoryId", Sort.Direction.ASC).named("product_category_idx"));
    }
}

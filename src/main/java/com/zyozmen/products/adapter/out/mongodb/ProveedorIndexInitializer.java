package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.LoteDocument;
import com.zyozmen.products.adapter.out.mongodb.document.TransaccionProveedorDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProveedorIndexInitializer implements ApplicationRunner {
    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        var lotes = mongoTemplate.indexOps(LoteDocument.class);
        lotes.ensureIndex(new Index().on("codigoLote", Sort.Direction.ASC).unique());
        lotes.ensureIndex(new Index().on("proveedorId", Sort.Direction.ASC)
                .on("fechaIngreso", Sort.Direction.ASC).on("_id", Sort.Direction.ASC));
        mongoTemplate.indexOps(TransaccionProveedorDocument.class).ensureIndex(new Index()
                .on("proveedorId", Sort.Direction.ASC).on("fecha", Sort.Direction.DESC)
                .on("_id", Sort.Direction.DESC));
    }
}

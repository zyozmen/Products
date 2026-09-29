package com.zyozmen.products.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri:}")
    private String uri;

    @Value("${spring.data.mongodb.database:GrowShop}")
    private String database;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    public MongoClient mongoClient() {
        String resolvedUri = StringUtils.trimWhitespace(uri);

        if (!StringUtils.hasText(resolvedUri) || resolvedUri.contains("<db_password>") || resolvedUri.contains("db_password") || resolvedUri.contains("REPLACE_ME")) {
            resolvedUri = "mongodb://localhost:27017/GrowShop?authSource=admin";
        }

        if (resolvedUri.startsWith("mongodb://") && (resolvedUri.contains("localhost") || resolvedUri.contains("127.0.0.1") || resolvedUri.contains("mongo"))) {
            return MongoClients.create(resolvedUri);
        }

        if (resolvedUri.startsWith("mongodb+srv://")) {
            return MongoClients.create(normalizeAtlasUri(resolvedUri));
        }

        throw new IllegalStateException("La URI de MongoDB debe usar el formato mongodb+srv:// para Atlas o mongodb:// para desarrollo local.");
    }

    private String normalizeAtlasUri(String rawUri) {
        try {
            URI parsedUri = new URI(rawUri);
            String query = parsedUri.getQuery();
            StringBuilder normalized = new StringBuilder(rawUri);

            if (!StringUtils.hasText(query)) {
                normalized.append("?retryWrites=true&w=majority&tls=true&authSource=admin");
            } else {
                if (!query.contains("retryWrites=")) {
                    normalized.append("&retryWrites=true");
                }
                if (!query.contains("w=")) {
                    normalized.append("&w=majority");
                }
                if (!query.contains("tls=")) {
                    normalized.append("&tls=true");
                }
                if (!query.contains("authSource=")) {
                    normalized.append("&authSource=admin");
                }
            }
            return normalized.toString();
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("La URI de MongoDB no tiene un formato válido", ex);
        }
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, database);
    }
}

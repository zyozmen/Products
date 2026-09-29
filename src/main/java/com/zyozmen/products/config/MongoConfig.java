package com.zyozmen.products.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri:}")
    private String uri;

    @Value("${MONGO_HOST:${MONGODB_HOST:}}")
    private String atlasHost;

    @Value("${MONGO_USERNAME:${MONGODB_USERNAME:}}")
    private String atlasUsername;

    @Value("${MONGO_PASSWORD:${MONGODB_PASSWORD:}}")
    private String atlasPassword;

    @Value("${spring.data.mongodb.database:GrowShop}")
    private String database;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    public MongoClient mongoClient() {
        // Se prefiere ensamblar la URI con usuario/password por separado: evita que un password con
        // caracteres reservados (@, :, /, etc.) desplace el '@' que separa credenciales del host.
        if (StringUtils.hasText(atlasHost) && StringUtils.hasText(atlasUsername) && StringUtils.hasText(atlasPassword)) {
            String encodedUser = UriUtils.encode(atlasUsername.trim(), StandardCharsets.UTF_8);
            String encodedPassword = UriUtils.encode(atlasPassword.trim(), StandardCharsets.UTF_8);
            String builtUri = "mongodb+srv://" + encodedUser + ":" + encodedPassword + "@" + atlasHost.trim();
            return MongoClients.create(normalizeAtlasUri(builtUri));
        }

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
        // Se busca el '?' de forma textual: java.net.URI falla si el password trae caracteres reservados (@, %, /, etc.)
        int queryIndex = rawUri.indexOf('?');
        String base = queryIndex >= 0 ? rawUri.substring(0, queryIndex) : rawUri;
        String query = queryIndex >= 0 ? rawUri.substring(queryIndex + 1) : "";

        // El driver exige un '/' entre el host y las opciones (mongodb+srv://host/?opt=...)
        int schemeEnd = base.indexOf("://") + 3;
        if (base.indexOf('/', schemeEnd) < 0) {
            base = base + "/";
        }

        StringBuilder normalized = new StringBuilder(base).append('?');

        if (!StringUtils.hasText(query)) {
            normalized.append("retryWrites=true&w=majority&tls=true&authSource=admin");
        } else {
            normalized.append(query);
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
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, database);
    }
}

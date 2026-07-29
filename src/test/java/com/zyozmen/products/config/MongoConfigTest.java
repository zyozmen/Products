package com.zyozmen.products.config;

import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MongoConfigTest {

    @Mock
    private MongoClient mongoClient;

    @Test
    void shouldExposeConfiguredDatabaseName() {
        MongoConfig config = new MongoConfig();
        ReflectionTestUtils.setField(config, "database", "products");

        assertThat(config.getDatabaseName()).isEqualTo("products");
    }

    @Test
    void shouldCreateMongoTemplateUsingProvidedClient() {
        MongoConfig config = new MongoConfig();
        ReflectionTestUtils.setField(config, "database", "products");

        org.springframework.data.mongodb.core.MongoTemplate template = config.mongoTemplate(mongoClient);

        assertThat(template).isNotNull();
        assertThat(template.getDb()).isNotNull();
        assertThat(template.getDb().getName()).isEqualTo("products");
    }
}

package com.zyozmen.products.config;

import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class MongoConfigTest {

    @Test
    void shouldCreateMongoClientFromLocalUri() {
        MongoConfig config = new MongoConfig();
        ReflectionTestUtils.setField(config, "uri", "mongodb://user:pass@mongo:27017/GrowShop");

        MongoClient client = config.mongoClient();

        assertThat(client).isNotNull();
    }

    @Test
    void shouldCreateMongoClientFromAtlasUri() {
        MongoConfig config = new MongoConfig();
        ReflectionTestUtils.setField(config, "uri", "mongodb+srv://user:pass@products-db-cluster.9wjnrah.mongodb.net/GrowShop");

        MongoClient client = config.mongoClient();

        assertThat(client).isNotNull();
    }
}

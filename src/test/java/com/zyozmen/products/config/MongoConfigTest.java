package com.zyozmen.products.config;

import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MongoConfigTest {

    @Test
    void shouldRejectLocalhostUrisToAvoidWrongTarget() {
        MongoConfig config = new MongoConfig();
        ReflectionTestUtils.setField(config, "uri", "mongodb://localhost:27017/GrowShop");

        assertThatThrownBy(config::mongoClient)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Atlas");
    }

    @Test
    void shouldCreateMongoClientFromAtlasUri() {
        MongoConfig config = new MongoConfig();
        ReflectionTestUtils.setField(config, "uri", "mongodb+srv://user:pass@products-db-cluster.9wjnrah.mongodb.net/GrowShop");

        MongoClient client = config.mongoClient();

        assertThat(client).isNotNull();
    }
}

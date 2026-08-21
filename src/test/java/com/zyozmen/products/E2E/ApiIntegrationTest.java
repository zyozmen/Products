package com.zyozmen.products.E2E;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class ApiIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void configureHttpClientAndCleanDatabase() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        mongoTemplate.getCollectionNames()
                .stream()
                .filter(collection -> !collection.startsWith("system."))
                .forEach(collection -> mongoTemplate.getCollection(collection).drop());
    }
}
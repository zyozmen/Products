package com.zyozmen.products.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void customOpenAPIShouldBuildInfoWithConfiguredValues() {
        OpenApiConfig config = new OpenApiConfig();
        ReflectionTestUtils.setField(config, "title", "Products API");
        ReflectionTestUtils.setField(config, "description", "API for products");
        ReflectionTestUtils.setField(config, "version", "1.0.0");
        ReflectionTestUtils.setField(config, "contactName", "ZYOZ");
        ReflectionTestUtils.setField(config, "contactEmail", "support@example.com");
        ReflectionTestUtils.setField(config, "contactUrl", "https://example.com");
        ReflectionTestUtils.setField(config, "licenseName", "MIT");
        ReflectionTestUtils.setField(config, "licenseUrl", "https://example.com/license");

        OpenAPI openAPI = config.customOpenAPI();

        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Products API");
        assertThat(openAPI.getInfo().getDescription()).isEqualTo("API for products");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(openAPI.getInfo().getContact()).isNotNull();
        assertThat(openAPI.getInfo().getContact().getName()).isEqualTo("ZYOZ");
        assertThat(openAPI.getInfo().getContact().getEmail()).isEqualTo("support@example.com");
        assertThat(openAPI.getInfo().getContact().getUrl()).isEqualTo("https://example.com");
        assertThat(openAPI.getInfo().getLicense()).isNotNull();
        assertThat(openAPI.getInfo().getLicense().getName()).isEqualTo("MIT");
        assertThat(openAPI.getInfo().getLicense().getUrl()).isEqualTo("https://example.com/license");
    }
}

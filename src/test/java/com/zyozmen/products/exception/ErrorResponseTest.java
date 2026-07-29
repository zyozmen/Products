package com.zyozmen.products.exception;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    void builderShouldPopulateFieldsAndAccessors() {
        LocalDateTime timestamp = LocalDateTime.of(2026, Month.JULY, 29, 12, 0);
        List<String> validationErrors = List.of("name is required");

        ErrorResponse response = ErrorResponse.builder()
                .status(400)
                .error("Bad Request")
                .message("Validation failed")
                .path("/products")
                .timestamp(timestamp)
                .validationErrors(validationErrors)
                .build();

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getError()).isEqualTo("Bad Request");
        assertThat(response.getMessage()).isEqualTo("Validation failed");
        assertThat(response.getPath()).isEqualTo("/products");
        assertThat(response.getTimestamp()).isEqualTo(timestamp);
        assertThat(response.getValidationErrors()).containsExactly("name is required");
    }
}

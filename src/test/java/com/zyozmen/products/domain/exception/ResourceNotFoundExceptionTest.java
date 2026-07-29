package com.zyozmen.products.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceNotFoundExceptionTest {

    @Test
    void shouldStoreMessage() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Product not found");

        assertThat(exception.getMessage()).isEqualTo("Product not found");
    }
}

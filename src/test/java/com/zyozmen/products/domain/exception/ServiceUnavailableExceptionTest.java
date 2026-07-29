package com.zyozmen.products.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceUnavailableExceptionTest {

    @Test
    void shouldStoreMessageAndCause() {
        RuntimeException cause = new RuntimeException("database down");
        ServiceUnavailableException exception = new ServiceUnavailableException("Service unavailable", cause);

        assertThat(exception.getMessage()).isEqualTo("Service unavailable");
        assertThat(exception.getCause()).isSameAs(cause);
    }
}

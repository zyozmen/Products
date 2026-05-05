package com.zyozmen.products.domain.exception;

/**
 * Excepción de dominio lanzada cuando el servicio de persistencia
 * no está disponible (circuit breaker abierto o reintentos agotados).
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

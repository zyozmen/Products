package com.zyozmen.products.domain.exception;

/**
 * Excepción de dominio. Vive en el dominio porque representa
 * una regla de negocio: el recurso solicitado no existe.
 * No depende de ningún framework.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}

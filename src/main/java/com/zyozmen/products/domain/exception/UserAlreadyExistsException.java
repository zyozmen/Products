package com.zyozmen.products.domain.exception;

/**
 * Excepción de dominio lanzada al intentar registrar un usuario cuyo
 * username ya está en uso.
 */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}

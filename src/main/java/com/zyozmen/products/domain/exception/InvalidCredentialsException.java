package com.zyozmen.products.domain.exception;

/**
 * Excepción de dominio lanzada cuando las credenciales de login
 * (username/password) no son válidas.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}

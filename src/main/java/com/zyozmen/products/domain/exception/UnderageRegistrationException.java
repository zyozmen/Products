package com.zyozmen.products.domain.exception;

/**
 * Excepción de dominio lanzada cuando el registro no puede completarse
 * porque el usuario no confirmó explícitamente ser mayor de edad.
 */
public class UnderageRegistrationException extends RuntimeException {

    public UnderageRegistrationException(String message) {
        super(message);
    }
}

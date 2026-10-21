package com.zyozmen.products.domain.exception;

/**
 * Excepción de dominio lanzada cuando un usuario válido intenta
 * iniciar sesión pero su cuenta ha sido inactivada por un administrador.
 */
public class UserInactiveException extends RuntimeException {

    public UserInactiveException(String message) {
        super(message);
    }
}

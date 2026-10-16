package com.zyozmen.products.domain.exception;

/**
 * Excepción de dominio lanzada cuando la sesión (token) expiró, fue revocada
 * o superó el límite de inactividad.
 */
public class SessionExpiredException extends RuntimeException {

    public static final String CODE = "SESSION_EXPIRED";

    public SessionExpiredException(String message) {
        super(message);
    }
}

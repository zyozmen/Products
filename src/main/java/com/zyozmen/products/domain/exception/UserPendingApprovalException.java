package com.zyozmen.products.domain.exception;

/**
 * Se lanza cuando un usuario registrado intenta iniciar sesión pero su
 * acceso aún no ha sido aprobado por un administrador.
 */
public class UserPendingApprovalException extends RuntimeException {

    public UserPendingApprovalException(String message) {
        super(message);
    }
}

package com.zyozmen.products.domain.exception;

/**
 * Excepción de dominio lanzada cuando se intenta modificar (inactivar/activar)
 * al administrador principal del sistema (username "admin"), lo cual está
 * prohibido para evitar que el sistema quede sin un administrador operativo.
 */
public class MainAdminProtectedException extends RuntimeException {

    public MainAdminProtectedException(String message) {
        super(message);
    }
}

package com.zyozmen.products.domain.model;

/**
 * Estado del ciclo de vida de una cuenta de usuario.
 * PENDIENTE: registrada, a la espera de aprobación de acceso por un ADMIN.
 */
public enum UserState {
    ACTIVO,
    INACTIVO,
    PENDIENTE
}

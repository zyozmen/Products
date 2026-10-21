package com.zyozmen.products.exception;

/**
 * Respuesta específica para el caso de login con usuario inactivo,
 * con la forma exacta requerida por el contrato de la API:
 * {"error": "user_inactive", "message": "..."}
 */
public record UserInactiveErrorResponse(String error, String message) {
}

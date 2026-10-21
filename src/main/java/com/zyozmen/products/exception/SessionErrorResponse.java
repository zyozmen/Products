package com.zyozmen.products.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error de sesión (401): token expirado, revocado o inactividad")
public record SessionErrorResponse(
        @Schema(example = "SESSION_EXPIRED") String code,
        @Schema(example = "Inactivity limit reached") String message) {
}

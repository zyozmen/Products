package com.zyozmen.products.domain.port.out;

import java.time.Instant;

/**
 * Puerto de salida para la lista de tokens revocados (sesiones cerradas).
 */
public interface RevokedTokenRepositoryPort {

    /**
     * Registra el token (jti) como revocado hasta su fecha natural de expiración.
     */
    void revoke(String tokenId, Instant expiresAt);

    boolean isRevoked(String tokenId);
}

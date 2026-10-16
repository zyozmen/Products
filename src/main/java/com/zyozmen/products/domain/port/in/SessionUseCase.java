package com.zyozmen.products.domain.port.in;

import com.zyozmen.products.domain.model.AuthResult;
import com.zyozmen.products.domain.model.LogoutReason;

/**
 * Puerto de entrada (Use Case) para el ciclo de vida de la sesión:
 * renovación del token y cierre/revocación.
 */
public interface SessionUseCase {

    /**
     * Renueva un token activo (firmado, no expirado, no revocado y dentro del
     * límite de inactividad) emitiendo uno nuevo y revocando el anterior.
     *
     * @throws com.zyozmen.products.domain.exception.SessionExpiredException si el token no es utilizable
     */
    AuthResult refresh(String token);

    /**
     * Revoca la sesión asociada al token. Es idempotente: tokens ya expirados
     * o inválidos no producen error.
     */
    void logout(String token, LogoutReason reason);
}

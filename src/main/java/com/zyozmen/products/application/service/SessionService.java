package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.SessionExpiredException;
import com.zyozmen.products.domain.model.AuthResult;
import com.zyozmen.products.domain.model.LogoutReason;
import com.zyozmen.products.domain.model.User;
import com.zyozmen.products.domain.model.UserState;
import com.zyozmen.products.domain.port.in.SessionUseCase;
import com.zyozmen.products.domain.port.out.RevokedTokenRepositoryPort;
import com.zyozmen.products.domain.port.out.UserRepositoryPort;
import com.zyozmen.products.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Servicio de aplicación para renovación y revocación de sesiones.
 *
 * El límite de inactividad es parametrizable (app.session.inactivity-timeout-minutes):
 * el frontend renueva el token mientras hay actividad, por lo que un token
 * emitido hace más de ese tiempo sin renovarse se considera inactivo.
 */
@Service
public class SessionService implements SessionUseCase {

    static final String MSG_EXPIRED = "Session expired";
    static final String MSG_INACTIVITY = "Inactivity limit reached";

    private final JwtService jwtService;
    private final RevokedTokenRepositoryPort revokedTokenPort;
    private final UserRepositoryPort userRepositoryPort;
    private final Duration inactivityTimeout;

    public SessionService(
            JwtService jwtService,
            RevokedTokenRepositoryPort revokedTokenPort,
            UserRepositoryPort userRepositoryPort,
            @Value("${app.session.inactivity-timeout-minutes:15}") long inactivityTimeoutMinutes) {
        this.jwtService = jwtService;
        this.revokedTokenPort = revokedTokenPort;
        this.userRepositoryPort = userRepositoryPort;
        this.inactivityTimeout = Duration.ofMinutes(inactivityTimeoutMinutes);
    }

    @Override
    public AuthResult refresh(String token) {
        if (token == null || !jwtService.isTokenValid(token)) {
            throw new SessionExpiredException(MSG_EXPIRED);
        }

        String tokenId = jwtService.extractJti(token);
        if (tokenId == null || revokedTokenPort.isRevoked(tokenId)) {
            throw new SessionExpiredException(MSG_EXPIRED);
        }

        if (jwtService.extractIssuedAt(token).plus(inactivityTimeout).isBefore(Instant.now())) {
            revokedTokenPort.revoke(tokenId, jwtService.extractExpiration(token));
            throw new SessionExpiredException(MSG_INACTIVITY);
        }

        User user = userRepositoryPort.findByUsername(jwtService.extractUsername(token))
                .filter(u -> u.getState() == UserState.ACTIVO)
                .orElseThrow(() -> new SessionExpiredException(MSG_EXPIRED));

        revokedTokenPort.revoke(tokenId, jwtService.extractExpiration(token));
        String newToken = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResult(newToken, user);
    }

    @Override
    public void logout(String token, LogoutReason reason) {
        if (token == null || !jwtService.isTokenValid(token)) {
            return;
        }
        String tokenId = jwtService.extractJti(token);
        if (tokenId != null) {
            revokedTokenPort.revoke(tokenId, jwtService.extractExpiration(token));
        }
    }
}

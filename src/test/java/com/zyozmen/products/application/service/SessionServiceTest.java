package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.SessionExpiredException;
import com.zyozmen.products.domain.model.AuthResult;
import com.zyozmen.products.domain.model.LogoutReason;
import com.zyozmen.products.domain.model.Role;
import com.zyozmen.products.domain.model.User;
import com.zyozmen.products.domain.port.out.RevokedTokenRepositoryPort;
import com.zyozmen.products.domain.port.out.UserRepositoryPort;
import com.zyozmen.products.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private RevokedTokenRepositoryPort revokedTokenPort;
    @Mock
    private UserRepositoryPort userRepositoryPort;

    private SessionService service() {
        return new SessionService(jwtService, revokedTokenPort, userRepositoryPort, 60);
    }

    @Test
    void refreshShouldIssueNewTokenAndRevokeOldOne() {
        Instant exp = Instant.now().plusSeconds(1800);
        User user = User.builder().username("juan").role(Role.CLIENTE).state(com.zyozmen.products.domain.model.UserState.ACTIVO).build();
        when(jwtService.isTokenValid("old")).thenReturn(true);
        when(jwtService.extractJti("old")).thenReturn("jti");
        when(revokedTokenPort.isRevoked("jti")).thenReturn(false);
        when(jwtService.extractIssuedAt("old")).thenReturn(Instant.now().minusSeconds(60));
        when(jwtService.extractExpiration("old")).thenReturn(exp);
        when(jwtService.extractUsername("old")).thenReturn("juan");
        when(userRepositoryPort.findByUsername("juan")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("juan", "CLIENTE")).thenReturn("new");

        AuthResult result = service().refresh("old");

        assertThat(result.getToken()).isEqualTo("new");
        verify(revokedTokenPort).revoke("jti", exp);
    }

    @Test
    void refreshShouldFailForInvalidToken() {
        when(jwtService.isTokenValid("bad")).thenReturn(false);

        assertThatThrownBy(() -> service().refresh("bad"))
                .isInstanceOf(SessionExpiredException.class);
        assertThatThrownBy(() -> service().refresh(null))
                .isInstanceOf(SessionExpiredException.class);
    }

    @Test
    void refreshShouldFailForRevokedToken() {
        when(jwtService.isTokenValid("t")).thenReturn(true);
        when(jwtService.extractJti("t")).thenReturn("jti");
        when(revokedTokenPort.isRevoked("jti")).thenReturn(true);

        assertThatThrownBy(() -> service().refresh("t"))
                .isInstanceOf(SessionExpiredException.class);
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void refreshShouldFailAndRevokeWhenInactivityLimitExceeded() {
        Instant exp = Instant.now().plusSeconds(1800);
        when(jwtService.isTokenValid("t")).thenReturn(true);
        when(jwtService.extractJti("t")).thenReturn("jti");
        when(revokedTokenPort.isRevoked("jti")).thenReturn(false);
        when(jwtService.extractIssuedAt("t")).thenReturn(Instant.now().minusSeconds(61 * 60));
        when(jwtService.extractExpiration("t")).thenReturn(exp);

        assertThatThrownBy(() -> service().refresh("t"))
                .isInstanceOf(SessionExpiredException.class)
                .hasMessage("Inactivity limit reached");
        verify(revokedTokenPort).revoke("jti", exp);
    }

    @Test
    void refreshShouldFailWhenUserIsInactive() {
        User user = User.builder().username("juan").role(Role.CLIENTE).state(com.zyozmen.products.domain.model.UserState.INACTIVO).build();
        when(jwtService.isTokenValid("t")).thenReturn(true);
        when(jwtService.extractJti("t")).thenReturn("jti");
        when(revokedTokenPort.isRevoked("jti")).thenReturn(false);
        when(jwtService.extractIssuedAt("t")).thenReturn(Instant.now());
        when(jwtService.extractUsername("t")).thenReturn("juan");
        when(userRepositoryPort.findByUsername("juan")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service().refresh("t"))
                .isInstanceOf(SessionExpiredException.class);
    }

    @Test
    void logoutShouldRevokeValidToken() {
        Instant exp = Instant.now().plusSeconds(1800);
        when(jwtService.isTokenValid("t")).thenReturn(true);
        when(jwtService.extractJti("t")).thenReturn("jti");
        when(jwtService.extractExpiration("t")).thenReturn(exp);

        service().logout("t", LogoutReason.USER);

        verify(revokedTokenPort).revoke("jti", exp);
    }

    @Test
    void logoutShouldBeIdempotentForInvalidOrMissingToken() {
        when(jwtService.isTokenValid("bad")).thenReturn(false);

        service().logout("bad", LogoutReason.INACTIVITY);
        service().logout(null, LogoutReason.USER);

        verify(revokedTokenPort, never()).revoke(any(), any());
    }
}

package com.zyozmen.products.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-secret-key-0123456789-0123456789-0123456789", 3600000L);

    @Test
    void generateTokenShouldProduceTokenWithExtractableUsernameAndRole() {
        String token = jwtService.generateToken("juan123", "CLIENTE");

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("juan123");
        assertThat(jwtService.extractRole(token)).isEqualTo("CLIENTE");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValidShouldReturnFalseForMalformedToken() {
        assertThat(jwtService.isTokenValid("not-a-valid-jwt")).isFalse();
    }

    @Test
    void isTokenValidShouldReturnFalseForExpiredToken() {
        JwtService expiringJwtService = new JwtService(
                "test-secret-key-0123456789-0123456789-0123456789", -1000L);

        String token = expiringJwtService.generateToken("juan123", "CLIENTE");

        assertThat(expiringJwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValidShouldReturnFalseForTokenSignedWithDifferentKey() {
        JwtService otherJwtService = new JwtService(
                "a-completely-different-secret-key-0123456789-0123456789", 3600000L);

        String token = otherJwtService.generateToken("juan123", "CLIENTE");

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void constructorShouldFallBackToDefaultSecretWhenBlank() {
        JwtService fallbackJwtService = new JwtService("", 3600000L);

        String token = fallbackJwtService.generateToken("admin", "ADMIN");

        assertThat(fallbackJwtService.isTokenValid(token)).isTrue();
    }
}

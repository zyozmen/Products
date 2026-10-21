package com.zyozmen.products.domain.model;

import java.util.Objects;

/**
 * Resultado de un inicio de sesión exitoso: token de sesión (JWT)
 * junto con el perfil del usuario autenticado.
 */
public class AuthResult {

    private final String token;
    private final User user;

    public AuthResult(String token, User user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public User getUser() {
        return user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthResult that = (AuthResult) o;
        return Objects.equals(token, that.token) && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, user);
    }
}

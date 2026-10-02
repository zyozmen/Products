package com.zyozmen.products.domain.port.in;

import com.zyozmen.products.domain.model.AuthResult;
import com.zyozmen.products.domain.model.User;

import java.util.List;

/**
 * Puerto de entrada (Inbound Port / Use Case) para la gestión de usuarios,
 * autenticación y administración de cuentas.
 */
public interface UserUseCase {

    /**
     * Registra un nuevo usuario. Fuerza role=cliente y active=true,
     * sin importar lo enviado en la petición.
     */
    User registrar(User user);

    /**
     * Autentica un usuario y genera un token de sesión (JWT).
     */
    AuthResult login(String username, String password);

    /**
     * Lista todos los usuarios del sistema (uso administrativo).
     */
    List<User> listarUsuarios();

    /**
     * Alterna (activa/inactiva) el estado de un usuario. No permite
     * modificar al administrador principal del sistema.
     */
    User toggleEstado(String username);
}

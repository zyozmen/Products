package com.zyozmen.products.domain.port.in;

import com.zyozmen.products.domain.model.AuthResult;
import com.zyozmen.products.domain.model.User;
import com.zyozmen.products.domain.model.UserState;

import java.util.List;

/**
 * Puerto de entrada (Inbound Port / Use Case) para la gestión de usuarios,
 * autenticación y administración de cuentas.
 */
public interface UserUseCase {

    /**
     * Registra un nuevo usuario. Fuerza role=cliente y state=PENDIENTE
     * (a la espera de aprobación de acceso), sin importar lo enviado.
     */
    User registrar(User user);

    /**
     * Autentica un usuario por correo electrónico y genera un token de sesión (JWT).
     */
    AuthResult login(String email, String password);

    /**
     * Lista todos los usuarios del sistema (uso administrativo).
     */
    List<User> listarUsuarios();

    /**
     * Cambia el estado (ACTIVO, INACTIVO, PENDIENTE) de un usuario. Operación
     * exclusiva del rol ADMIN; no permite modificar al administrador principal.
     */
    User cambiarEstado(String username, UserState nuevoEstado);

    /**
     * Obtiene el perfil de un usuario por su username.
     */
    User obtenerPorUsername(String username);

    /**
     * Actualiza los datos de perfil (nombre, apellido, dirección, teléfono
     * y, opcionalmente, contraseña) de un usuario. Solo se modifican los
     * campos que vengan informados (no nulos/no en blanco); la contraseña,
     * si se envía, se hashea antes de persistirse.
     */
    User actualizarPerfil(
            String username,
            String nombre,
            String apellido,
            String direccion,
            String telefono,
            String password);
}

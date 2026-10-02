package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.InvalidCredentialsException;
import com.zyozmen.products.domain.exception.MainAdminProtectedException;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.exception.UnderageRegistrationException;
import com.zyozmen.products.domain.exception.UserAlreadyExistsException;
import com.zyozmen.products.domain.exception.UserInactiveException;
import com.zyozmen.products.domain.model.AuthResult;
import com.zyozmen.products.domain.model.Role;
import com.zyozmen.products.domain.model.User;
import com.zyozmen.products.domain.port.in.UserUseCase;
import com.zyozmen.products.domain.port.out.UserRepositoryPort;
import com.zyozmen.products.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación del caso de uso de usuarios (servicio de aplicación).
 *
 * Orquesta registro, autenticación y administración de cuentas usando
 * únicamente el modelo de dominio y los puertos de salida.
 */
@Service
@RequiredArgsConstructor
public class UserService implements UserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.admin.username:admin}")
    private String mainAdminUsername;

    @Override
    @Transactional
    public User registrar(User user) {
        if (userRepositoryPort.existsByUsername(user.getUsername())) {
            throw new UserAlreadyExistsException("El nombre de usuario ya está registrado");
        }

        if (userRepositoryPort.existsByTipoIdentificacionAndNumeroIdentificacion(
                user.getTipoIdentificacion(), user.getNumeroIdentificacion())) {
            throw new UserAlreadyExistsException(
                    "Ya existe un usuario registrado con ese tipo y número de identificación");
        }

        if (!Boolean.TRUE.equals(user.getMayorDeEdad())) {
            throw new UnderageRegistrationException(
                    "Debe confirmar ser mayor de edad (18 años o más) para registrarse");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // Independiente de lo recibido en el payload, todo registro público
        // queda forzado a rol cliente y cuenta activa.
        user.setRole(Role.CLIENTE);
        user.setActive(true);

        return userRepositoryPort.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResult login(String username, String password) {
        User user = userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales inválidas"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Credenciales inválidas");
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new UserInactiveException(
                    "El usuario está inactivo. Por favor contacte al administrador");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResult(token, user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> listarUsuarios() {
        return userRepositoryPort.findAll();
    }

    @Override
    @Transactional
    public User toggleEstado(String username) {
        if (mainAdminUsername.equals(username)) {
            throw new MainAdminProtectedException(
                    "No se puede inactivar/activar al administrador principal del sistema");
        }

        User user = userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado: " + username));

        user.setActive(!Boolean.TRUE.equals(user.getActive()));

        return userRepositoryPort.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User obtenerPorUsername(String username) {
        return userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado: " + username));
    }

    @Override
    @Transactional
    public User actualizarPerfil(
            String username,
            String nombre,
            String apellido,
            String direccion,
            String telefono,
            String password) {

        User user = userRepositoryPort.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado: " + username));

        if (nombre != null && !nombre.isBlank()) {
            user.setNombre(nombre);
        }
        if (apellido != null && !apellido.isBlank()) {
            user.setApellido(apellido);
        }
        if (direccion != null && !direccion.isBlank()) {
            user.setDireccion(direccion);
        }
        if (telefono != null && !telefono.isBlank()) {
            user.setTelefono(telefono);
        }
        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }

        return userRepositoryPort.save(user);
    }
}

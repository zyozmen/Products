package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida (Outbound Port / Repository Port) para la persistencia
 * de usuarios.
 */
public interface UserRepositoryPort {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByTipoIdentificacionAndNumeroIdentificacion(
            com.zyozmen.products.domain.model.IdentificationType tipoIdentificacion,
            String numeroIdentificacion);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    User save(User user);

    List<User> findAll();
}

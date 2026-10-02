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

    boolean existsByTipoIdentificacionAndNumeroIdentificacion(
            com.zyozmen.products.domain.model.TipoIdentificacion tipoIdentificacion,
            String numeroIdentificacion);

    Optional<User> findByUsername(String username);

    User save(User user);

    List<User> findAll();
}

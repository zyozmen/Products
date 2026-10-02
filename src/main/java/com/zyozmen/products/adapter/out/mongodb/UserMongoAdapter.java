package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.mapper.UserMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.UserMongoRepository;
import com.zyozmen.products.domain.model.TipoIdentificacion;
import com.zyozmen.products.domain.model.User;
import com.zyozmen.products.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida (Outbound Adapter) MongoDB para usuarios.
 * Implementa el puerto de salida definido en el dominio usando Spring Data MongoDB.
 */
@Service
@RequiredArgsConstructor
public class UserMongoAdapter implements UserRepositoryPort {

    private final UserMongoRepository mongoRepository;
    private final UserMongoMapper mapper;

    @Override
    public boolean existsByUsername(String username) {
        return mongoRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByTipoIdentificacionAndNumeroIdentificacion(
            TipoIdentificacion tipoIdentificacion, String numeroIdentificacion) {
        if (tipoIdentificacion == null || numeroIdentificacion == null) {
            return false;
        }
        return mongoRepository.existsByTipoIdentificacionAndNumeroIdentificacion(
                tipoIdentificacion.name(), numeroIdentificacion);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return mongoRepository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public User save(User user) {
        String existingId = mongoRepository.findByUsername(user.getUsername())
                .map(com.zyozmen.products.adapter.out.mongodb.document.UserMongoDocument::getId)
                .orElse(null);
        return mapper.toDomain(mongoRepository.save(mapper.toDocument(user, existingId)));
    }

    @Override
    public List<User> findAll() {
        return mongoRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}

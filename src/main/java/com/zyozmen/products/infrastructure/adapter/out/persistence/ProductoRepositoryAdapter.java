package com.zyozmen.products.infrastructure.adapter.out.persistence;

import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.port.out.ProductoRepositoryPort;
import com.zyozmen.products.infrastructure.adapter.out.persistence.mapper.ProductoPersistenceMapper;
import com.zyozmen.products.infrastructure.adapter.out.persistence.repository.ProductoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de salida (Outbound Adapter / Driven Adapter).
 *
 * Implementa el puerto de salida definido en el dominio usando
 * Spring Data JPA. El dominio nunca ve esta clase: solo conoce la
 * interfaz ProductoRepositoryPort.
 * Si se cambia MySQL por MongoDB, solo se reemplaza este adaptador;
 * el dominio y la aplicación no se modifican.
 */
@Component
@RequiredArgsConstructor
public class ProductoRepositoryAdapter implements ProductoRepositoryPort {

    private final ProductoJpaRepository jpaRepository;
    private final ProductoPersistenceMapper mapper;

    @Override
    public List<Producto> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Producto> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Producto save(Producto producto) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(producto)));
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}

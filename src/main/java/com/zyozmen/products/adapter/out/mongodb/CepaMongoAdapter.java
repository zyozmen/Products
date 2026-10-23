package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.CepaDocument;
import com.zyozmen.products.adapter.out.mongodb.mapper.ImagenMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.CepaMongoRepository;
import com.zyozmen.products.domain.model.Cepa;
import com.zyozmen.products.domain.port.out.CepaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CepaMongoAdapter implements CepaRepositoryPort {
    private final CepaMongoRepository repository;

    @Override
    public Cepa save(Cepa c) {
        return toDomain(repository.save(new CepaDocument(c.id(), c.nombre(), Cepa.normalizar(c.nombre()),
                ImagenMongoMapper.toDocument(c.fotografia()), c.createdAt(), c.updatedAt(), c.version(), c.categoryIds())));
    }

    @Override
    public Optional<Cepa> findById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El ID de cepa es obligatorio");
        }
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Page<Cepa> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDomain);
    }

    private Cepa toDomain(CepaDocument c) {
        return new Cepa(c.id(), c.nombre(), ImagenMongoMapper.toDomain(c.fotografia()),
                c.createdAt(), c.updatedAt(), c.version(), c.categoryIds());
    }
}

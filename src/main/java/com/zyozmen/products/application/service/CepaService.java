package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.Cepa;
import com.zyozmen.products.domain.model.Imagen;
import com.zyozmen.products.domain.port.in.CepaUseCase;
import com.zyozmen.products.domain.port.out.CepaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CepaService implements CepaUseCase {
    private final CepaRepositoryPort repository;

    @Override
    public Cepa crear(String nombre) {
        Cepa.normalizar(nombre);
        var now = Instant.now();
        return repository.save(new Cepa(UUID.randomUUID().toString(), nombre.strip().replaceAll("\\s+", " "),
                null, now, now, null));
    }

    @Override
    public Cepa actualizar(String id, String nombre, Long version) {
        Cepa.normalizar(nombre);
        var anterior = obtener(id);
        if (version == null || !version.equals(anterior.version())) {
            throw new OptimisticLockingFailureException("La cepa fue modificada; vuelva a consultarla");
        }
        return repository.save(new Cepa(id, nombre.strip().replaceAll("\\s+", " "), anterior.fotografia(),
                anterior.createdAt(), Instant.now(), anterior.version()));
    }

    @Override
    public Cepa obtener(String id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cepa no encontrada"));
    }

    @Override
    public Page<Cepa> listar(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page debe ser >= 0 y size entre 1 y 100");
        }
        return repository.findAll(PageRequest.of(page, size, Sort.by("nombreNormalizado", "id")));
    }

    @Override
    public Cepa guardarFotografia(String id, Imagen fotografia) {
        if (fotografia == null) {
            throw new IllegalArgumentException("La foto principal es obligatoria para cepas");
        }
        var anterior = obtener(id);
        return repository.save(new Cepa(id, anterior.nombre(), fotografia, anterior.createdAt(),
                Instant.now(), anterior.version()));
    }
}

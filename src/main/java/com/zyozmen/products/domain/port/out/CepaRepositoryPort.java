package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.Cepa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CepaRepositoryPort {
    Cepa save(Cepa cepa);
    Optional<Cepa> findById(String id);
    Page<Cepa> findAll(Pageable pageable);
}

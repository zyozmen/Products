package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.Provider;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProveedorRepositoryPort {
    Provider save(Provider proveedor);

    Optional<Provider> findById(String id);

    boolean existsByNit(String nit);

    Page<Provider> buscar(String texto, Pageable pageable);
}

package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.Proveedor;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProveedorRepositoryPort {
    Proveedor save(Proveedor proveedor);

    Optional<Proveedor> findById(String id);

    boolean existsByNit(String nit);

    Page<Proveedor> buscar(String texto, Pageable pageable);
}

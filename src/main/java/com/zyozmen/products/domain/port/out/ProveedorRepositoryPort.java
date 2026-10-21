package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.Proveedor;

import java.util.Optional;

public interface ProveedorRepositoryPort {
    Proveedor save(Proveedor proveedor);

    Optional<Proveedor> findById(String id);

    boolean existsByNit(String nit);
}

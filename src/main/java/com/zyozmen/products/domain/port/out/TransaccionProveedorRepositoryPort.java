package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.ProviderTransaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransaccionProveedorRepositoryPort {
    ProviderTransaction insert(ProviderTransaction transaccion);

    Page<ProviderTransaction> findAbonosByProveedorId(String proveedorId, Pageable pageable);
}

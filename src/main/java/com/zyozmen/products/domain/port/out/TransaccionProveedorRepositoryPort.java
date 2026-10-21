package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.TransaccionProveedor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransaccionProveedorRepositoryPort {
    TransaccionProveedor insert(TransaccionProveedor transaccion);

    Page<TransaccionProveedor> findAbonosByProveedorId(String proveedorId, Pageable pageable);
}

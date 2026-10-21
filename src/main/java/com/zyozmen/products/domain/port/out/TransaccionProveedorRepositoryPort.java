package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.TransaccionProveedor;

public interface TransaccionProveedorRepositoryPort {
    TransaccionProveedor save(TransaccionProveedor transaccion);
}

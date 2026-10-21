package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.Lote;

import java.util.List;
import java.util.Optional;

public interface LoteRepositoryPort {
    Lote save(Lote lote);

    Optional<Lote> findLoteById(String id);

    boolean existsByCodigoLote(String codigoLote);

    /** Lotes del proveedor con saldo pendiente, del mÃ¡s antiguo al mÃ¡s reciente. */
    List<Lote> findPendientesByProveedorId(String proveedorId);
}

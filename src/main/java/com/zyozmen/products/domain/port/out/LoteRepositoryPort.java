package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.Lote;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LoteRepositoryPort {
    Lote save(Lote lote);

    Optional<Lote> findLoteById(String id);

    Page<Lote> findLotesByProveedorId(String proveedorId, String buscar, Pageable pageable);

    long countPagadosByProveedorId(String proveedorId);

    long countPorPagarByProveedorId(String proveedorId);

    /** Lotes del proveedor con saldo pendiente, del mÃ¡s antiguo al mÃ¡s reciente. */
    List<Lote> findPendientesByProveedorId(String proveedorId);
}

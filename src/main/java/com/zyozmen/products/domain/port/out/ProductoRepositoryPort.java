package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.Producto;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida (Outbound Port / Repository Port).
 *
 * Interfaz definida en el dominio que describe qué operaciones de
 * persistencia necesita la aplicación. El dominio NO sabe nada de
 * JPA ni de MySQL: solo declara el contrato.
 * La implementación concreta (ProductoRepositoryAdapter) vive en
 * la capa de infraestructura e inyecta Spring Data hacia adentro.
 */
public interface ProductoRepositoryPort {

    List<Producto> findAll();

    Optional<Producto> findById(Long id);

    Producto save(Producto producto);

    boolean existsById(Long id);

    void deleteById(Long id);
}

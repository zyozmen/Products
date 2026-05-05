package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.port.in.ProductoUseCase;
import com.zyozmen.products.domain.port.out.ProductoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación del caso de uso (servicio de aplicación).
 *
 * Orquesta la lógica de negocio usando únicamente el modelo de dominio
 * y el puerto de salida. No conoce ni HTTP ni JPA: solo coordina
 * objetos del dominio y delega la persistencia al puerto de salida.
 */
@Service
@RequiredArgsConstructor
public class ProductoService implements ProductoUseCase {

    private final ProductoRepositoryPort productoRepositoryPort;

    @Override
    @Transactional(readOnly = true)
    public List<Producto> listarTodos() {
        return productoRepositoryPort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Producto obtenerPorId(Long id) {
        return productoRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public Producto crear(Producto producto) {
        return productoRepositoryPort.save(producto);
    }

    @Override
    @Transactional
    public Producto actualizar(Long id, Producto producto) {
        Producto existente = productoRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto no encontrado con ID: " + id));
        existente.setNombre(producto.getNombre());
        existente.setDescripcion(producto.getDescripcion());
        existente.setPrecio(producto.getPrecio());
        return productoRepositoryPort.save(existente);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!productoRepositoryPort.existsById(id)) {
            throw new ResourceNotFoundException("Producto no encontrado con ID: " + id);
        }
        productoRepositoryPort.deleteById(id);
    }
}

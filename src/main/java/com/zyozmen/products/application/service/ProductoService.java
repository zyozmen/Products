package com.zyozmen.products.application.service;

import com.zyozmen.products.adapter.out.mongodb.ProductoMongoAdapter;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.Category;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.port.in.ProductoUseCase;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    @Autowired
    private ProductoMongoAdapter productoRepositoryPort;

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

        producto.setCreatedAt(Instant.now());
        producto.setUpdatedAt(Instant.now());
        return productoRepositoryPort.save(producto);
    }

    @Override
    @Transactional
    public Producto actualizar(Long id, Producto producto) {
        Producto existente = productoRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto no encontrado con ID: " + id));

        // Keep path id as source of truth while updating every other field.
        existente.setId(id);
        existente.setName(producto.getName());
        existente.setSlug(producto.getSlug());
        existente.setDescription(producto.getDescription());
        existente.setSku(producto.getSku());
        existente.setStatus(producto.getStatus());
        existente.setCategories(producto.getCategories());
        existente.setPrice(producto.getPrice());
        existente.setRanking(producto.getRanking());
        existente.setRecentComments(producto.getRecentComments());
        existente.setHasMoreComments(producto.getHasMoreComments());
        existente.setCreatedAt(producto.getCreatedAt());
        existente.setUpdatedAt(Instant.now());

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

    @Override
    public List<Producto> listarDestacados() {
        return productoRepositoryPort.findFeatured();
    }

    @Override
    public List<Category> listarCategorias() {
        return productoRepositoryPort.findAllCategories();
    }
}

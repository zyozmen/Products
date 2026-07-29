package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.Category;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.port.in.ProductoUseCase;
import com.zyozmen.products.domain.port.out.ProductoRepositoryPort;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    private final ProductoRepositoryPort productoRepositoryPort;

    @Override
    @Transactional(readOnly = true)
    public List<Producto> listarTodos() {
        return productoRepositoryPort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Producto> listarTodos(Pageable pageable) {
        return productoRepositoryPort.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Producto> listarTodosPorCategorias(List<Long> categoryIds, Pageable pageable) {
        return productoRepositoryPort.findAllByCategoryIds(categoryIds, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Producto> listarTodosFiltrado(
            List<Long> categoryIds,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating,
            BigDecimal maxRating,
            String name,
            Pageable pageable) {
        return productoRepositoryPort.findAllFiltered(
                categoryIds,
                minPrice,
                maxPrice,
                minRating,
                maxRating,
                name,
                pageable);
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

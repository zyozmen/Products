package com.zyozmen.products.domain.port.in;

import com.zyozmen.products.domain.model.Category;
import com.zyozmen.products.domain.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Puerto de entrada (Inbound Port / Use Case).
 *
 * Define los casos de uso que el dominio expone hacia los adaptadores
 * de entrada (controladores, mensajería, CLI, etc.).
 * Al trabajar únicamente con el modelo de dominio, el núcleo queda
 * completamente desacoplado de los detalles de transporte HTTP.
 */
public interface ProductoUseCase {

    List<Producto> listarTodos();

    Page<Producto> listarTodos(Pageable pageable);

    Page<Producto> listarTodosPorCategorias(List<Long> categoryIds, Pageable pageable);

        Page<Producto> listarTodosFiltrado(
            List<Long> categoryIds,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating,
            BigDecimal maxRating,
            String name,
            Pageable pageable);

    Producto obtenerPorId(Long id);

    Producto crear(Producto producto);

    Producto actualizar(Long id, Producto producto);

    void eliminar(Long id);

    List<Producto> listarDestacados();

    List<Category> listarCategorias();


}

package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.Category;
import com.zyozmen.products.domain.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida (Outbound Port / Repository Port).
 *
 * Interfaz definida en el dominio que describe qué operaciones de
 * persistencia necesita la aplicación.
 * 
 */
public interface ProductoRepositoryPort {

    List<Producto> findAll();

    Page<Producto> findAll(Pageable pageable);

    Page<Producto> findAllByCategoryIds(List<Long> categoryIds, Pageable pageable);

    /** Lista solo productos activos. */
    default Page<Producto> findAllFiltered(
            List<Long> categoryIds,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating,
            BigDecimal maxRating,
            String name,
            Pageable pageable) {
        return findAllFiltered(categoryIds, minPrice, maxPrice, minRating, maxRating, name, false, pageable);
    }

    Page<Producto> findAllFiltered(
            List<Long> categoryIds,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating,
            BigDecimal maxRating,
            String name,
            boolean includeInactive,
            Pageable pageable);

    Optional<Producto> findById(Long id);

    Producto save(Producto producto);

    boolean existsById(Long id);

    void deleteById(Long id);

    List<Producto> findFeatured();

    List<Category> findAllCategories();
}

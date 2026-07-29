package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.Category;
import com.zyozmen.products.domain.model.Producto;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProductoRepositoryPortTest {

    @Test
    void shouldExposeContractMethodsForRepositoryOperations() {
        ProductoRepositoryPort port = new ProductoRepositoryPort() {
            @Override
            public List<Producto> findAll() {
                return List.of();
            }

            @Override
            public Page<Producto> findAll(Pageable pageable) {
                return new PageImpl<>(List.of(), pageable, 0);
            }

            @Override
            public Page<Producto> findAllByCategoryIds(List<Long> categoryIds, Pageable pageable) {
                return new PageImpl<>(List.of(), pageable, 0);
            }

            @Override
            public Page<Producto> findAllFiltered(List<Long> categoryIds, BigDecimal minPrice, BigDecimal maxPrice, BigDecimal minRating, BigDecimal maxRating, String name, Pageable pageable) {
                return new PageImpl<>(List.of(), pageable, 0);
            }

            @Override
            public Optional<Producto> findById(Long id) {
                return Optional.empty();
            }

            @Override
            public Producto save(Producto producto) {
                return producto;
            }

            @Override
            public boolean existsById(Long id) {
                return false;
            }

            @Override
            public void deleteById(Long id) {
            }

            @Override
            public List<Producto> findFeatured() {
                return List.of();
            }

            @Override
            public List<Category> findAllCategories() {
                return List.of();
            }
        };

        assertThat(port.findAll()).isEmpty();
        assertThat(port.findAll(Pageable.unpaged()).getTotalElements()).isZero();
        assertThat(port.findAllByCategoryIds(List.of(1L), Pageable.unpaged()).getTotalElements()).isZero();
        assertThat(port.findAllFiltered(List.of(1L), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, "name", Pageable.unpaged()).getTotalElements()).isZero();
        assertThat(port.findById(1L)).isEmpty();
        Producto producto = Producto.builder().id("1").build();
        assertThat(port.save(producto)).isSameAs(producto);
        assertThat(port.existsById(1L)).isFalse();
        port.deleteById(1L);
        assertThat(port.findFeatured()).isEmpty();
        assertThat(port.findAllCategories()).isEmpty();
    }
}

package com.zyozmen.products.domain.port.in;

import com.zyozmen.products.domain.model.Category;
import com.zyozmen.products.domain.model.Producto;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductoUseCaseTest {

    @Test
    void shouldExposeContractMethodsForProductOperations() {
        ProductoUseCase useCase = new ProductoUseCase() {
            @Override
            public List<Producto> listarTodos() {
                return List.of();
            }

            @Override
            public Page<Producto> listarTodos(Pageable pageable) {
                return new PageImpl<>(List.of(), pageable, 0);
            }

            @Override
            public Page<Producto> listarTodosPorCategorias(List<Long> categoryIds, Pageable pageable) {
                return new PageImpl<>(List.of(), pageable, 0);
            }

            @Override
            public Page<Producto> listarTodosFiltrado(List<Long> categoryIds, BigDecimal minPrice, BigDecimal maxPrice, BigDecimal minRating, BigDecimal maxRating, String name, Pageable pageable) {
                return new PageImpl<>(List.of(), pageable, 0);
            }

            @Override
            public Producto obtenerPorId(Long id) {
                return null;
            }

            @Override
            public Producto crear(Producto producto) {
                return producto;
            }

            @Override
            public Producto actualizar(Long id, Producto producto) {
                return producto;
            }

            @Override
            public void eliminar(Long id) {
            }

            @Override
            public List<Producto> listarDestacados() {
                return List.of();
            }

            @Override
            public List<Category> listarCategorias() {
                return List.of();
            }
        };

        assertThat(useCase.listarTodos()).isEmpty();
        assertThat(useCase.listarTodos(Pageable.unpaged()).getTotalElements()).isZero();
        assertThat(useCase.listarTodosPorCategorias(List.of(1L), Pageable.unpaged()).getTotalElements()).isZero();
        assertThat(useCase.listarTodosFiltrado(List.of(1L), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, "name", Pageable.unpaged()).getTotalElements()).isZero();
        assertThat(useCase.obtenerPorId(1L)).isNull();
        Producto producto = Producto.builder().id("1").build();
        assertThat(useCase.crear(producto)).isSameAs(producto);
        assertThat(useCase.actualizar(1L, producto)).isSameAs(producto);
        useCase.eliminar(1L);
        assertThat(useCase.listarDestacados()).isEmpty();
        assertThat(useCase.listarCategorias()).isEmpty();
    }
}

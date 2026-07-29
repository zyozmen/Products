package com.zyozmen.products.domain.port.in;

import com.zyozmen.products.domain.model.Producto;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ProductoUseCaseTest {

    @Mock
    private ProductoUseCase useCase;

    @Test
    void shouldExposeContractMethodsForProductOperations() {
        Producto producto = Producto.builder().id("1").build();


        when(useCase.listarTodos()).thenReturn(List.of());
        when(useCase.listarTodos(Pageable.unpaged())).thenReturn(new PageImpl<>(List.of()));
        when(useCase.listarTodosPorCategorias(List.of(1L), Pageable.unpaged())).thenReturn(new PageImpl<>(List.of()));
        when(useCase.listarTodosFiltrado(List.of(1L), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, "name", Pageable.unpaged())).thenReturn(new PageImpl<>(List.of()));
        when(useCase.obtenerPorId(1L)).thenReturn(null);
        when(useCase.crear(producto)).thenReturn(producto);
        when(useCase.actualizar(1L, producto)).thenReturn(producto);


        assertThat(useCase.listarTodos()).isEmpty();
        assertThat(useCase.listarTodos(Pageable.unpaged()).getTotalElements()).isZero();
        assertThat(useCase.listarTodosPorCategorias(List.of(1L), Pageable.unpaged()).getTotalElements()).isZero();
        assertThat(useCase.listarTodosFiltrado(List.of(1L), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, "name", Pageable.unpaged()).getTotalElements()).isZero();
        assertThat(useCase.obtenerPorId(1L)).isNull();
        
        assertThat(useCase.crear(producto)).isSameAs(producto);
        assertThat(useCase.actualizar(1L, producto)).isSameAs(producto);
        useCase.eliminar(1L);
        assertThat(useCase.listarDestacados()).isEmpty();
        assertThat(useCase.listarCategorias()).isEmpty();
    }
}

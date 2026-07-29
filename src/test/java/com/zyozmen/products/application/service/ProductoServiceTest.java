package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.Category;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.port.out.ProductoRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepositoryPort productoRepositoryPort;

    @InjectMocks
    private ProductoService productoService;

    @Test
    void listarTodosShouldReturnRepositoryProducts() {
        Producto producto = Producto.builder().id("1").name("Headphones").build();
        when(productoRepositoryPort.findAll()).thenReturn(List.of(producto));

        List<Producto> result = productoService.listarTodos();

        assertThat(result).containsExactly(producto);
        verify(productoRepositoryPort).findAll();
    }

    @Test
    void listarTodosPageableShouldDelegateToRepository() {
        Pageable pageable = PageRequest.of(0, 5);
        Producto producto = Producto.builder().id("2").name("Keyboard").build();
        Page<Producto> expectedPage = new PageImpl<>(List.of(producto), pageable, 1);

        when(productoRepositoryPort.findAll(pageable)).thenReturn(expectedPage);

        Page<Producto> result = productoService.listarTodos(pageable);

        assertThat(result.getContent()).containsExactly(producto);
        verify(productoRepositoryPort).findAll(pageable);
    }

    @Test
    void listarTodosPorCategoriasShouldDelegateToRepository() {
        Pageable pageable = PageRequest.of(0, 3);
        Producto producto = Producto.builder().id("3").name("Monitor").build();
        Page<Producto> expectedPage = new PageImpl<>(List.of(producto), pageable, 1);

        when(productoRepositoryPort.findAllByCategoryIds(List.of(1L, 2L), pageable)).thenReturn(expectedPage);

        Page<Producto> result = productoService.listarTodosPorCategorias(List.of(1L, 2L), pageable);

        assertThat(result.getContent()).containsExactly(producto);
        verify(productoRepositoryPort).findAllByCategoryIds(List.of(1L, 2L), pageable);
    }

    @Test
    void obtenerPorIdShouldReturnProductWhenPresent() {
        Producto producto = Producto.builder().id("4").name("Speaker").build();
        when(productoRepositoryPort.findById(4L)).thenReturn(Optional.of(producto));

        Producto result = productoService.obtenerPorId(4L);

        assertThat(result).isSameAs(producto);
        verify(productoRepositoryPort).findById(4L);
    }

    @Test
    void obtenerPorIdShouldThrowWhenProductIsMissing() {
        when(productoRepositoryPort.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.obtenerPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void crearShouldSetTimestampsAndSaveProduct() {
        Producto producto = Producto.builder().name("Mouse").build();
        when(productoRepositoryPort.save(producto)).thenReturn(producto);

        Producto result = productoService.crear(producto);

        assertThat(result).isSameAs(producto);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(productoRepositoryPort).save(producto);
    }

    @Test
    void actualizarShouldReplaceFieldsAndPersistUpdatedProduct() {
        Producto existente = Producto.builder().id("10").name("Old").slug("old").description("old desc").sku("SKU-OLD").status("inactive").build();
        Producto updates = Producto.builder().name("New").slug("new").description("new desc").sku("SKU-NEW").status("active").build();

        when(productoRepositoryPort.findById(10L)).thenReturn(Optional.of(existente));
        when(productoRepositoryPort.save(existente)).thenReturn(existente);

        Producto result = productoService.actualizar(10L, updates);

        assertThat(result).isSameAs(existente);
        assertThat(existente.getId()).isEqualTo("10");
        assertThat(existente.getName()).isEqualTo("New");
        assertThat(existente.getUpdatedAt()).isNotNull();
        verify(productoRepositoryPort).save(existente);
    }

    @Test
    void listarTodosFiltradoShouldDelegateToRepository() {
        Pageable pageable = PageRequest.of(0, 5);
        Producto producto = Producto.builder().id("2").name("Keyboard").build();
        Page<Producto> expectedPage = new PageImpl<>(List.of(producto), pageable, 1);

        when(productoRepositoryPort.findAllFiltered(List.of(1L), BigDecimal.TEN, BigDecimal.valueOf(100), BigDecimal.ONE, BigDecimal.valueOf(5), "key", pageable))
                .thenReturn(expectedPage);

        Page<Producto> result = productoService.listarTodosFiltrado(List.of(1L), BigDecimal.TEN, BigDecimal.valueOf(100), BigDecimal.ONE, BigDecimal.valueOf(5), "key", pageable);

        assertThat(result.getContent()).containsExactly(producto);
        verify(productoRepositoryPort).findAllFiltered(List.of(1L), BigDecimal.TEN, BigDecimal.valueOf(100), BigDecimal.ONE, BigDecimal.valueOf(5), "key", pageable);
    }

    @Test
    void actualizarShouldThrowWhenProductIsMissing() {
        Producto updates = Producto.builder().name("New").build();

        when(productoRepositoryPort.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.actualizar(42L, updates))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void eliminarShouldThrowWhenProductDoesNotExist() {
        when(productoRepositoryPort.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> productoService.eliminar(7L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("7");
    }

    @Test
    void eliminarShouldDeleteExistingProduct() {
        when(productoRepositoryPort.existsById(8L)).thenReturn(true);

        productoService.eliminar(8L);

        verify(productoRepositoryPort).deleteById(8L);
    }

    @Test
    void listarDestacadosShouldDelegateToRepository() {
        Producto producto = Producto.builder().id("5").name("Console").build();
        when(productoRepositoryPort.findFeatured()).thenReturn(List.of(producto));

        List<Producto> result = productoService.listarDestacados();

        assertThat(result).containsExactly(producto);
        verify(productoRepositoryPort).findFeatured();
    }

    @Test
    void listarCategoriasShouldReturnRepositoryCategories() {
        Category category = Category.builder().categoryId(1L).name("Audio").slug("audio").build();
        when(productoRepositoryPort.findAllCategories()).thenReturn(List.of(category));

        List<Category> result = productoService.listarCategorias();

        assertThat(result).containsExactly(category);
        verify(productoRepositoryPort).findAllCategories();
    }
}

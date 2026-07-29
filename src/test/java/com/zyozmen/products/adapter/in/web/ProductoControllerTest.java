package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.ProductoListItemDTO;
import com.zyozmen.products.adapter.in.web.mapper.ProductoWebMapper;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.port.in.ProductoUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoControllerTest {

    @Mock
    private ProductoUseCase productoUseCase;

    @Mock
    private ProductoWebMapper productoWebMapper;

    @InjectMocks
    private ProductoController productoController;

    @Test
    void listarTodosWithoutFiltersShouldDelegateToListarTodosAndMapTheResult() {
        Pageable pageable = PageRequest.of(0, 15);
        Producto producto = Producto.builder().id("1").name("Headphones").build();
        ProductoListItemDTO dto = ProductoListItemDTO.builder().id("1").name("Headphones").build();
        Page<Producto> productoPage = new PageImpl<>(List.of(producto), pageable, 1);

        when(productoUseCase.listarTodos(pageable)).thenReturn(productoPage);
        when(productoWebMapper.toListItemDTO(producto)).thenReturn(dto);

        ResponseEntity<Page<ProductoListItemDTO>> response = productoController.listarTodos(
                0,
                15,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).containsExactly(dto);

        verify(productoUseCase).listarTodos(pageable);
        verify(productoUseCase, never()).listarTodosPorCategorias(anyList(), any(Pageable.class));
        verify(productoUseCase, never()).listarTodosFiltrado(anyList(), any(), any(), any(), any(), any(), any(Pageable.class));
        verify(productoWebMapper).toListItemDTO(producto);
    }

    @Test
    void listarTodosWithCategoryFilterShouldDelegateToListarTodosPorCategorias() {
        Pageable pageable = PageRequest.of(0, 15);
        Producto producto = Producto.builder().id("2").name("Keyboard").build();
        ProductoListItemDTO dto = ProductoListItemDTO.builder().id("2").name("Keyboard").build();
        Page<Producto> productoPage = new PageImpl<>(List.of(producto), pageable, 1);

        when(productoUseCase.listarTodosPorCategorias(eq(List.of(1L, 2L)), any(Pageable.class))).thenReturn(productoPage);
        when(productoWebMapper.toListItemDTO(producto)).thenReturn(dto);

        ResponseEntity<Page<ProductoListItemDTO>> response = productoController.listarTodos(
                0,
                15,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(1L, 2L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).containsExactly(dto);

        verify(productoUseCase).listarTodosPorCategorias(eq(List.of(1L, 2L)), any(Pageable.class));
        verify(productoUseCase, never()).listarTodos(any(Pageable.class));
        verify(productoUseCase, never()).listarTodosFiltrado(anyList(), any(), any(), any(), any(), any(), any(Pageable.class));
    }
}

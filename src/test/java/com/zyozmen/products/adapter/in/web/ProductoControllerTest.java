package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.CategoryDTO;
import com.zyozmen.products.adapter.in.web.dto.ProductoListItemDTO;
import com.zyozmen.products.adapter.in.web.dto.ProductoRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.ProductoResponseDTO;
import com.zyozmen.products.adapter.in.web.mapper.ProductoWebMapper;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.port.in.ProductoUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
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

    @Test
    void listarTodosWithFiltersShouldDelegateToListarTodosFiltrado() {
        Pageable pageable = PageRequest.of(0, 15);
        Producto producto = Producto.builder().id("3").name("Mouse").build();
        ProductoListItemDTO dto = ProductoListItemDTO.builder().id("3").name("Mouse").build();
        Page<Producto> productoPage = new PageImpl<>(List.of(producto), pageable, 1);

        when(productoUseCase.listarTodosFiltrado(eq(List.of(1L, 2L)), eq(BigDecimal.valueOf(100)), eq(BigDecimal.valueOf(500)), eq(BigDecimal.valueOf(3.5)), eq(BigDecimal.valueOf(5.0)), eq("head"), any(Pageable.class)))
                .thenReturn(productoPage);
        when(productoWebMapper.toListItemDTO(producto)).thenReturn(dto);

        ResponseEntity<Page<ProductoListItemDTO>> response = productoController.listarTodos(
                0,
                15,
                "price",
                "asc",
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(500),
                BigDecimal.valueOf(3.5),
                BigDecimal.valueOf(5.0),
                "head",
                List.of(1L, 2L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).containsExactly(dto);

        verify(productoUseCase).listarTodosFiltrado(eq(List.of(1L, 2L)), eq(BigDecimal.valueOf(100)), eq(BigDecimal.valueOf(500)), eq(BigDecimal.valueOf(3.5)), eq(BigDecimal.valueOf(5.0)), eq("head"), any(Pageable.class));
        verify(productoUseCase, never()).listarTodos(any(Pageable.class));
        verify(productoUseCase, never()).listarTodosPorCategorias(anyList(), any(Pageable.class));
    }

    @Test
    void listarTodosShouldUsePriceSortWhenRequested() {
        Producto producto = Producto.builder().id("4").name("Monitor").build();
        ProductoListItemDTO dto = ProductoListItemDTO.builder().id("4").name("Monitor").build();
        Page<Producto> productoPage = new PageImpl<>(List.of(producto), PageRequest.of(0, 10), 1);

        when(productoUseCase.listarTodos(any(Pageable.class))).thenReturn(productoPage);
        when(productoWebMapper.toListItemDTO(producto)).thenReturn(dto);

        productoController.listarTodos(1, 10, "price", "asc", null, null, null, null, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productoUseCase).listarTodos(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("price.current")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("price.current").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void listarTodosShouldUseRatingSortWhenRequested() {
        Producto producto = Producto.builder().id("5").name("Speaker").build();
        ProductoListItemDTO dto = ProductoListItemDTO.builder().id("5").name("Speaker").build();
        Page<Producto> productoPage = new PageImpl<>(List.of(producto), PageRequest.of(0, 10), 1);

        when(productoUseCase.listarTodos(any(Pageable.class))).thenReturn(productoPage);
        when(productoWebMapper.toListItemDTO(producto)).thenReturn(dto);

        productoController.listarTodos(0, 10, "rating", "desc", null, null, null, null, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productoUseCase).listarTodos(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("ranking.averageRating")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("ranking.averageRating").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void listarTodosShouldUseUnsortedWhenSortIsUnknown() {
        Producto producto = Producto.builder().id("6").name("Camera").build();
        ProductoListItemDTO dto = ProductoListItemDTO.builder().id("6").name("Camera").build();
        Page<Producto> productoPage = new PageImpl<>(List.of(producto), PageRequest.of(0, 10), 1);

        when(productoUseCase.listarTodos(any(Pageable.class))).thenReturn(productoPage);
        when(productoWebMapper.toListItemDTO(producto)).thenReturn(dto);

        productoController.listarTodos(0, 10, "unknown", "desc", null, null, null, null, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productoUseCase).listarTodos(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.unsorted());
    }

    @Test
    void obtenerPorIdShouldReturnMappedResponse() {
        Producto producto = Producto.builder().id("7").name("Laptop").build();
        ProductoResponseDTO responseDTO = ProductoResponseDTO.builder().id("7").name("Laptop").build();

        when(productoUseCase.obtenerPorId(7L)).thenReturn(producto);
        when(productoWebMapper.toResponseDTO(producto)).thenReturn(responseDTO);

        ResponseEntity<ProductoResponseDTO> response = productoController.obtenerPorId(7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(responseDTO);
        verify(productoUseCase).obtenerPorId(7L);
        verify(productoWebMapper).toResponseDTO(producto);
    }

    @Test
    void crearShouldCreateAndMapResponse() {
        ProductoRequestDTO requestDTO = ProductoRequestDTO.builder().name("Tablet").build();
        Producto producto = Producto.builder().id("8").name("Tablet").build();
        ProductoResponseDTO responseDTO = ProductoResponseDTO.builder().id("8").name("Tablet").build();

        when(productoWebMapper.toDomain(requestDTO)).thenReturn(producto);
        when(productoUseCase.crear(producto)).thenReturn(producto);
        when(productoWebMapper.toResponseDTO(producto)).thenReturn(responseDTO);

        ResponseEntity<ProductoResponseDTO> response = productoController.crear(requestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(responseDTO);
        verify(productoWebMapper).toDomain(requestDTO);
        verify(productoUseCase).crear(producto);
        verify(productoWebMapper).toResponseDTO(producto);
    }

    @Test
    void actualizarShouldUpdateAndMapResponse() {
        ProductoRequestDTO requestDTO = ProductoRequestDTO.builder().name("Smartwatch").build();
        Producto producto = Producto.builder().id("9").name("Smartwatch").build();
        ProductoResponseDTO responseDTO = ProductoResponseDTO.builder().id("9").name("Smartwatch").build();

        when(productoWebMapper.toDomain(requestDTO)).thenReturn(producto);
        when(productoUseCase.actualizar(9L, producto)).thenReturn(producto);
        when(productoWebMapper.toResponseDTO(producto)).thenReturn(responseDTO);

        ResponseEntity<ProductoResponseDTO> response = productoController.actualizar(9L, requestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(responseDTO);
        verify(productoWebMapper).toDomain(requestDTO);
        verify(productoUseCase).actualizar(9L, producto);
        verify(productoWebMapper).toResponseDTO(producto);
    }

    @Test
    void eliminarShouldDelegateAndReturnNoContent() {
        ResponseEntity<Void> response = productoController.eliminar(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(productoUseCase).eliminar(10L);
    }

    @Test
    void obtenerDestacadosShouldReturnMappedFeaturedProducts() {
        Producto producto = Producto.builder().id("11").name("Console").build();
        ProductoResponseDTO responseDTO = ProductoResponseDTO.builder().id("11").name("Console").build();

        when(productoUseCase.listarDestacados()).thenReturn(List.of(producto));
        when(productoWebMapper.toResponseDTO(producto)).thenReturn(responseDTO);

        ResponseEntity<List<ProductoResponseDTO>> response = productoController.obtenerDestacados();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(responseDTO);
        verify(productoUseCase).listarDestacados();
        verify(productoWebMapper).toResponseDTO(producto);
    }

    @Test
    void obtenerCategoriasShouldReturnMappedCategories() {
        com.zyozmen.products.domain.model.Category category = com.zyozmen.products.domain.model.Category.builder().categoryId(1L).name("Electronics").slug("electronics").build();
        CategoryDTO categoryDTO = CategoryDTO.builder().categoryId(1L).name("Electronics").slug("electronics").build();

        when(productoUseCase.listarCategorias()).thenReturn(List.of(category));
        when(productoWebMapper.toCategoryDTO(category)).thenReturn(categoryDTO);

        ResponseEntity<List<CategoryDTO>> response = productoController.obtenerCategorias();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(categoryDTO);
        verify(productoUseCase).listarCategorias();
        verify(productoWebMapper).toCategoryDTO(category);
    }
}

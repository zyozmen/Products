package com.zyozmen.products.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.port.in.ProductoUseCase;
import com.zyozmen.products.adapter.in.web.dto.ProductoRequestDTO;
import com.zyozmen.products.adapter.in.web.mapper.ProductoWebMapper;
import com.zyozmen.products.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductoController (web adapter) - Pruebas unitarias")
class ProductoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductoUseCase productoUseCase;

    @Mock
    private ProductoWebMapper productoWebMapper;

    @InjectMocks
    private ProductoController productoController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Producto producto;
    private ProductoRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        producto = Producto.builder()
                .id(1L)
                .nombre("Laptop Dell XPS 15")
                .descripcion("Procesador Intel Core i7, 16GB RAM")
                .precio(new BigDecimal("1299.99"))
                .build();

        requestDTO = ProductoRequestDTO.builder()
                .nombre("Laptop Dell XPS 15")
                .descripcion("Procesador Intel Core i7, 16GB RAM")
                .precio(new BigDecimal("1299.99"))
                .build();
    }

    // ─── GET /api/productos ───────────────────────────

    @Test
    @DisplayName("GET /api/productos - retorna 200 con lista mapeada")
    void listarTodos_retorna200() throws Exception {
        when(productoUseCase.listarTodos()).thenReturn(List.of(producto));

        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new ProductoController(productoUseCase, new ProductoWebMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Laptop Dell XPS 15"));
    }

    @Test
    @DisplayName("GET /api/productos - retorna 200 con lista vacía")
    void listarTodos_retorna200ListaVacia() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new ProductoController(productoUseCase, new ProductoWebMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        when(productoUseCase.listarTodos()).thenReturn(List.of());

        mvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ─── GET /api/productos/{id} ──────────────────────

    @Test
    @DisplayName("GET /api/productos/{id} - retorna 200 cuando el producto existe")
    void obtenerPorId_productoExiste_retorna200() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new ProductoController(productoUseCase, new ProductoWebMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        when(productoUseCase.obtenerPorId(1L)).thenReturn(producto);

        mvc.perform(get("/api/productos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Laptop Dell XPS 15"))
                .andExpect(jsonPath("$.precio").value(1299.99));
    }

    @Test
    @DisplayName("GET /api/productos/{id} - retorna 404 cuando no existe")
    void obtenerPorId_productoNoExiste_retorna404() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new ProductoController(productoUseCase, new ProductoWebMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        when(productoUseCase.obtenerPorId(99L))
                .thenThrow(new ResourceNotFoundException("Producto no encontrado con ID: 99"));

        mvc.perform(get("/api/productos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Producto no encontrado con ID: 99"));
    }

    // ─── POST /api/productos ──────────────────────────

    @Test
    @DisplayName("POST /api/productos - retorna 201 con producto creado")
    void crear_datosValidos_retorna201() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new ProductoController(productoUseCase, new ProductoWebMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        when(productoUseCase.crear(any(Producto.class))).thenReturn(producto);

        mvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Laptop Dell XPS 15"));
    }

    @Test
    @DisplayName("POST /api/productos - retorna 400 cuando nombre está vacío")
    void crear_nombreVacio_retorna400() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new ProductoController(productoUseCase, new ProductoWebMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        ProductoRequestDTO invalido = ProductoRequestDTO.builder()
                .nombre("").precio(new BigDecimal("10.00")).build();

        mvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productoUseCase);
    }

    @Test
    @DisplayName("POST /api/productos - retorna 400 cuando precio es nulo")
    void crear_precioNulo_retorna400() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new ProductoController(productoUseCase, new ProductoWebMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        ProductoRequestDTO invalido = ProductoRequestDTO.builder()
                .nombre("Laptop").precio(null).build();

        mvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productoUseCase);
    }

    // ─── PUT /api/productos/{id} ──────────────────────

    @Test
    @DisplayName("PUT /api/productos/{id} - retorna 200 con producto actualizado")
    void actualizar_datosValidos_retorna200() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new ProductoController(productoUseCase, new ProductoWebMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        when(productoUseCase.actualizar(eq(1L), any(Producto.class))).thenReturn(producto);

        mvc.perform(put("/api/productos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/productos/{id} - retorna 404 cuando el producto no existe")
    void actualizar_productoNoExiste_retorna404() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new ProductoController(productoUseCase, new ProductoWebMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        when(productoUseCase.actualizar(eq(99L), any(Producto.class)))
                .thenThrow(new ResourceNotFoundException("Producto no encontrado con ID: 99"));

        mvc.perform(put("/api/productos/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isNotFound());
    }

    // ─── DELETE /api/productos/{id} ───────────────────

    @Test
    @DisplayName("DELETE /api/productos/{id} - retorna 204 al eliminar")
    void eliminar_productoExiste_retorna204() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new ProductoController(productoUseCase, new ProductoWebMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        doNothing().when(productoUseCase).eliminar(1L);

        mvc.perform(delete("/api/productos/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/productos/{id} - retorna 404 cuando no existe")
    void eliminar_productoNoExiste_retorna404() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new ProductoController(productoUseCase, new ProductoWebMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        doThrow(new ResourceNotFoundException("Producto no encontrado con ID: 99"))
                .when(productoUseCase).eliminar(99L);

        mvc.perform(delete("/api/productos/99"))
                .andExpect(status().isNotFound());
    }
}

package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.port.out.ProductoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductoService (application) - Pruebas unitarias")
class ProductoServiceTest {

    @Mock
    private ProductoRepositoryPort productoRepositoryPort;

    @InjectMocks
    private ProductoService productoService;

    private Producto producto;

    @BeforeEach
    void setUp() {
        producto = Producto.builder()
                .id(1L)
                .nombre("Laptop Dell XPS 15")
                .descripcion("Procesador Intel Core i7, 16GB RAM")
                .precio(new BigDecimal("1299.99"))
                .build();
    }

    // ─── listarTodos ────────────────────────────────

    @Test
    @DisplayName("listarTodos - retorna todos los productos del puerto de salida")
    void listarTodos_retornaListaDeProductos() {
        when(productoRepositoryPort.findAll()).thenReturn(List.of(producto));

        List<Producto> resultado = productoService.listarTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Laptop Dell XPS 15");
        verify(productoRepositoryPort).findAll();
    }

    @Test
    @DisplayName("listarTodos - retorna lista vacía cuando no hay productos")
    void listarTodos_retornaListaVacia() {
        when(productoRepositoryPort.findAll()).thenReturn(List.of());

        assertThat(productoService.listarTodos()).isEmpty();
    }

    // ─── obtenerPorId ────────────────────────────────

    @Test
    @DisplayName("obtenerPorId - retorna el producto cuando existe")
    void obtenerPorId_productoExiste_retornaProducto() {
        when(productoRepositoryPort.findById(1L)).thenReturn(Optional.of(producto));

        Producto resultado = productoService.obtenerPorId(1L);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Laptop Dell XPS 15");
    }

    @Test
    @DisplayName("obtenerPorId - lanza ResourceNotFoundException cuando no existe")
    void obtenerPorId_productoNoExiste_lanzaExcepcion() {
        when(productoRepositoryPort.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.obtenerPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ─── crear ────────────────────────────────────────

    @Test
    @DisplayName("crear - delega al puerto y retorna el producto persistido")
    void crear_productoValido_retornaProductoCreado() {
        when(productoRepositoryPort.save(producto)).thenReturn(producto);

        Producto resultado = productoService.crear(producto);

        assertThat(resultado.getNombre()).isEqualTo("Laptop Dell XPS 15");
        verify(productoRepositoryPort).save(producto);
    }

    // ─── actualizar ───────────────────────────────────

    @Test
    @DisplayName("actualizar - producto existe, actualiza campos y retorna resultado")
    void actualizar_productoExiste_retornaProductoActualizado() {
        Producto cambios = Producto.builder()
                .nombre("Laptop Actualizada")
                .descripcion("Nueva descripción")
                .precio(new BigDecimal("999.00"))
                .build();

        Producto productoActualizado = Producto.builder()
                .id(1L).nombre("Laptop Actualizada")
                .descripcion("Nueva descripción")
                .precio(new BigDecimal("999.00"))
                .build();

        when(productoRepositoryPort.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepositoryPort.save(any(Producto.class))).thenReturn(productoActualizado);

        Producto resultado = productoService.actualizar(1L, cambios);

        assertThat(resultado.getNombre()).isEqualTo("Laptop Actualizada");
        assertThat(resultado.getPrecio()).isEqualByComparingTo(new BigDecimal("999.00"));
        verify(productoRepositoryPort).findById(1L);
        verify(productoRepositoryPort).save(any(Producto.class));
    }

    @Test
    @DisplayName("actualizar - producto no existe, lanza ResourceNotFoundException")
    void actualizar_productoNoExiste_lanzaExcepcion() {
        when(productoRepositoryPort.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.actualizar(99L, producto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(productoRepositoryPort, never()).save(any());
    }

    // ─── eliminar ─────────────────────────────────────

    @Test
    @DisplayName("eliminar - producto existe, invoca deleteById en el puerto")
    void eliminar_productoExiste_eliminaCorrectamente() {
        when(productoRepositoryPort.existsById(1L)).thenReturn(true);
        doNothing().when(productoRepositoryPort).deleteById(1L);

        productoService.eliminar(1L);

        verify(productoRepositoryPort).existsById(1L);
        verify(productoRepositoryPort).deleteById(1L);
    }

    @Test
    @DisplayName("eliminar - producto no existe, lanza ResourceNotFoundException")
    void eliminar_productoNoExiste_lanzaExcepcion() {
        when(productoRepositoryPort.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productoService.eliminar(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(productoRepositoryPort, never()).deleteById(any());
    }
}

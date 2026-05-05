package com.zyozmen.products.adapter.in.web.mapper;

import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.adapter.in.web.dto.ProductoRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.ProductoResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductoWebMapper - Pruebas unitarias")
class ProductoWebMapperTest {

    private ProductoWebMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ProductoWebMapper();
    }

    @Test
    @DisplayName("toDomain - convierte RequestDTO al modelo de dominio")
    void toDomain_requestDTO_retornaProductoDominio() {
        ProductoRequestDTO dto = ProductoRequestDTO.builder()
                .nombre("Laptop Dell XPS 15")
                .descripcion("Intel Core i7")
                .precio(new BigDecimal("1299.99"))
                .build();

        Producto resultado = mapper.toDomain(dto);

        assertThat(resultado.getId()).isNull();
        assertThat(resultado.getNombre()).isEqualTo("Laptop Dell XPS 15");
        assertThat(resultado.getDescripcion()).isEqualTo("Intel Core i7");
        assertThat(resultado.getPrecio()).isEqualByComparingTo(new BigDecimal("1299.99"));
    }

    @Test
    @DisplayName("toDomain - descripcion nula se mantiene nula")
    void toDomain_descripcionNula_mapeoCorrectamente() {
        ProductoRequestDTO dto = ProductoRequestDTO.builder()
                .nombre("Mouse").descripcion(null).precio(new BigDecimal("25.00")).build();

        Producto resultado = mapper.toDomain(dto);

        assertThat(resultado.getDescripcion()).isNull();
    }

    @Test
    @DisplayName("toResponseDTO - convierte dominio a ResponseDTO")
    void toResponseDTO_producto_retornaResponseDTO() {
        Producto producto = Producto.builder()
                .id(1L).nombre("Laptop Dell XPS 15")
                .descripcion("Intel Core i7").precio(new BigDecimal("1299.99"))
                .build();

        ProductoResponseDTO resultado = mapper.toResponseDTO(producto);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Laptop Dell XPS 15");
        assertThat(resultado.getDescripcion()).isEqualTo("Intel Core i7");
        assertThat(resultado.getPrecio()).isEqualByComparingTo(new BigDecimal("1299.99"));
    }

    @Test
    @DisplayName("toResponseDTO - descripcion nula se mantiene nula")
    void toResponseDTO_descripcionNula_mapeoCorrectamente() {
        Producto producto = Producto.builder()
                .id(2L).nombre("Teclado").descripcion(null).precio(new BigDecimal("89.99"))
                .build();

        ProductoResponseDTO resultado = mapper.toResponseDTO(producto);

        assertThat(resultado.getDescripcion()).isNull();
    }
}

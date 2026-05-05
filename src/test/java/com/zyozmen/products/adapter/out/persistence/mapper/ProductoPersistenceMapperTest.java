package com.zyozmen.products.adapter.out.persistence.mapper;

import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.adapter.out.persistence.entity.ProductoJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductoPersistenceMapper - Pruebas unitarias")
class ProductoPersistenceMapperTest {

    private ProductoPersistenceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ProductoPersistenceMapper();
    }

    @Test
    @DisplayName("toDomain - convierte JpaEntity al modelo de dominio")
    void toDomain_entidadJpa_retornaProductoDominio() {
        ProductoJpaEntity entity = ProductoJpaEntity.builder()
                .id(1L).nombre("Laptop Dell XPS 15")
                .descripcion("Intel Core i7").precio(new BigDecimal("1299.99"))
                .build();

        Producto resultado = mapper.toDomain(entity);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Laptop Dell XPS 15");
        assertThat(resultado.getDescripcion()).isEqualTo("Intel Core i7");
        assertThat(resultado.getPrecio()).isEqualByComparingTo(new BigDecimal("1299.99"));
    }

    @Test
    @DisplayName("toDomain - descripcion nula se mantiene nula")
    void toDomain_descripcionNula_mapeoCorrectamente() {
        ProductoJpaEntity entity = ProductoJpaEntity.builder()
                .id(2L).nombre("Mouse").descripcion(null).precio(new BigDecimal("25.00"))
                .build();

        assertThat(mapper.toDomain(entity).getDescripcion()).isNull();
    }

    @Test
    @DisplayName("toEntity - convierte modelo de dominio a JpaEntity")
    void toEntity_productoDominio_retornaEntidadJpa() {
        Producto producto = Producto.builder()
                .id(1L).nombre("Laptop Dell XPS 15")
                .descripcion("Intel Core i7").precio(new BigDecimal("1299.99"))
                .build();

        ProductoJpaEntity resultado = mapper.toEntity(producto);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Laptop Dell XPS 15");
        assertThat(resultado.getDescripcion()).isEqualTo("Intel Core i7");
        assertThat(resultado.getPrecio()).isEqualByComparingTo(new BigDecimal("1299.99"));
    }

    @Test
    @DisplayName("toEntity - id nulo (nuevo producto) se mapea correctamente")
    void toEntity_idNulo_mapeoCorrectamente() {
        Producto producto = Producto.builder()
                .nombre("Teclado").precio(new BigDecimal("89.99")).build();

        ProductoJpaEntity resultado = mapper.toEntity(producto);

        assertThat(resultado.getId()).isNull();
        assertThat(resultado.getNombre()).isEqualTo("Teclado");
    }

    @Test
    @DisplayName("toDomain + toEntity - conversión de ida y vuelta es consistente")
    void roundTrip_dominioAEntidadYVuelta_conservaTodosLosCampos() {
        Producto original = Producto.builder()
                .id(5L).nombre("Monitor").descripcion("4K UHD").precio(new BigDecimal("499.00"))
                .build();

        Producto resultado = mapper.toDomain(mapper.toEntity(original));

        assertThat(resultado).isEqualTo(original);
    }
}

package com.zyozmen.products.adapter.in.graphql;

import com.zyozmen.products.adapter.in.web.mapper.ProductoWebMapper;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.port.in.ProductoUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@GraphQlTest(controllers = ProductGraphqlAdapter.class)
@Import({ProductoWebMapper.class, ProductGraphqlExceptionHandler.class})
@DisplayName("ProductGraphqlAdapter - GraphQL tests")
class ProductGraphqlAdapterTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockBean
    private ProductoUseCase productoUseCase;

    @Test
    @DisplayName("listarTodos returns mapped product list")
    void listarTodos_returnsProductList() {
        Producto producto = Producto.builder()
                .id(1L)
                .nombre("Laptop Dell XPS 15")
                .descripcion("Intel Core i7, 16GB RAM")
                .precio(new BigDecimal("1299.99"))
                .build();

        when(productoUseCase.listarTodos()).thenReturn(List.of(producto));

        graphQlTester.document("""
                query {
                  listarTodos {
                    id
                    nombre
                    descripcion
                    precio
                  }
                }
                """)
                .execute()
                .path("listarTodos[0].nombre").entity(String.class).isEqualTo("Laptop Dell XPS 15")
                .path("listarTodos[0].precio").entity(Double.class).isEqualTo(1299.99);
    }

    @Test
    @DisplayName("crear mutation creates product")
    void crear_createsProduct() {
        Producto creado = Producto.builder()
                .id(10L)
                .nombre("Mouse Logitech")
                .descripcion("Wireless")
                .precio(new BigDecimal("149.99"))
                .build();

        when(productoUseCase.crear(org.mockito.ArgumentMatchers.any(Producto.class))).thenReturn(creado);

        graphQlTester.document("""
                mutation Create($input: ProductInput!) {
                  crear(input: $input) {
                    id
                    nombre
                    descripcion
                    precio
                  }
                }
                """)
                .variable("input", Map.of(
                        "nombre", "Mouse Logitech",
                        "descripcion", "Wireless",
                        "precio", 149.99
                ))
                .execute()
                .path("crear.id").entity(String.class).isEqualTo("10")
                .path("crear.nombre").entity(String.class).isEqualTo("Mouse Logitech")
                .path("crear.precio").entity(Double.class).isEqualTo(149.99);
    }

    @Test
    @DisplayName("obtenerPorId returns NOT_FOUND GraphQL error extensions")
    void obtenerPorId_notFound_returnsGraphQlExtensions() {
        when(productoUseCase.obtenerPorId(99L))
                .thenThrow(new ResourceNotFoundException("Producto no encontrado con ID: 99"));

        graphQlTester.document("""
                query {
                  obtenerPorId(id: 99) {
                    id
                    nombre
                  }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).hasSize(1);
                  ResponseError error = errors.get(0);
                    assertThat(error.getMessage()).contains("Producto no encontrado con ID: 99");
                    assertThat(error.getExtensions()).containsEntry("code", "NOT_FOUND");
                    assertThat(error.getExtensions()).containsEntry("status", 404);
                    assertThat(error.getExtensions()).containsEntry("error", "Not Found");
                });
    }
}

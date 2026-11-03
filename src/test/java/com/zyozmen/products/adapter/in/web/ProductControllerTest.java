package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.domain.model.Product;
import com.zyozmen.products.domain.port.in.ProductUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductControllerTest {

    @Test
    void delegatesProductOperationsAndReturnsListCount() {
        ProductUseCase useCase = mock(ProductUseCase.class);
        ProductController controller = new ProductController(useCase);
        Product product = new Product("product-1", "Tangerine");
        when(useCase.crear("Tangerine", List.of(7L))).thenReturn(product);
        when(useCase.actualizar("product-1", "Tangerine", 3L, List.of(7L))).thenReturn(product);
        when(useCase.listar(1, 2)).thenReturn(
                new PageImpl<>(List.of(product), PageRequest.of(1, 2), 5));

        assertThat(controller.crear(new ProductController.ProductRequest("Tangerine", null, List.of(7L))))
                .isSameAs(product);
        assertThat(controller.actualizar("product-1",
                new ProductController.ProductRequest("Tangerine", 3L, List.of(7L)))).isSameAs(product);
        when(useCase.obtener("product-1")).thenReturn(product);
        assertThat(controller.obtener("product-1")).isSameAs(product);

        var response = controller.listar(1, 2);
        assertThat(response.getBody()).containsExactly(product);
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isEqualTo("5");

        verify(useCase).crear("Tangerine", List.of(7L));
        verify(useCase).actualizar("product-1", "Tangerine", 3L, List.of(7L));
    }
}

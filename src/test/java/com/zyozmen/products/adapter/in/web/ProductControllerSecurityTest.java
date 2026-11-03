package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.config.SecurityConfig;
import com.zyozmen.products.domain.model.Product;
import com.zyozmen.products.domain.port.in.ProductUseCase;
import com.zyozmen.products.domain.port.out.RevokedTokenRepositoryPort;
import com.zyozmen.products.security.JwtAuthenticationFilter;
import com.zyozmen.products.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ProductControllerSecurityTest {
    @Autowired private MockMvc mvc;
    @MockBean private ProductUseCase useCase;
    @MockBean private JwtService jwt;
    @MockBean private RevokedTokenRepositoryPort revokedTokens;

    @Test
    void productAdministrationRequiresAdminRole() throws Exception {
        mvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/products").with(user("client").roles("CLIENTE")))
                .andExpect(status().isForbidden());

        Product product = new Product("product-1", "Tangerine");
        when(useCase.listar(0, 20)).thenReturn(new PageImpl<>(
                List.of(product), PageRequest.of(0, 20), 1));
        mvc.perform(get("/api/products").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        verify(useCase).listar(0, 20);
    }
}

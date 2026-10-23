package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.config.SecurityConfig;
import com.zyozmen.products.domain.model.EstadoPagoLote;
import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.port.in.PresentacionUseCase;
import com.zyozmen.products.domain.port.out.RevokedTokenRepositoryPort;
import com.zyozmen.products.security.JwtAuthenticationFilter;
import com.zyozmen.products.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PresentacionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PresentacionSecurityTest {
    @Autowired private MockMvc mvc;
    @MockBean private PresentacionUseCase useCase;
    @MockBean private JwtService jwt;
    @MockBean private RevokedTokenRepositoryPort revokedTokens;

    private static final String URL = "/api/v1/lotes/l1/contenedores/c1/presentaciones";
    private static final String BODY = """
            [{"sku":"SAT-5","nombre":"Bolsa","contenidoUnidad":5,"unidadesCreadas":1,"precioVenta":25000}]
            """;

    @Test
    void anonimoNoPuedeEmpaquetar() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(useCase);
    }

    @Test
    void clienteNoPuedeEmpaquetar() throws Exception {
        mvc.perform(post(URL).with(user("cliente").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(useCase);
    }

    @Test
    void administradorPuedeEmpaquetar() throws Exception {
        when(useCase.empaquetar(anyString(), anyString(), anyList())).thenReturn(
                Lote.builder().id("l1").estadoPago(EstadoPagoLote.PENDIENTE).contenedores(List.of()).build());
        mvc.perform(post(URL).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());
    }
}

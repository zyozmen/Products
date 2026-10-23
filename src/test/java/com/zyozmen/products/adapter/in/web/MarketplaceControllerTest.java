package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.config.SecurityConfig;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.port.in.MarketplaceUseCase;
import com.zyozmen.products.domain.model.MarketplaceCatalogFilter;
import com.zyozmen.products.domain.port.out.RevokedTokenRepositoryPort;
import com.zyozmen.products.security.JwtAuthenticationFilter;
import com.zyozmen.products.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MarketplaceController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class MarketplaceControllerTest {
    @Autowired private MockMvc mvc;
    @MockBean private MarketplaceUseCase marketplace;
    @MockBean private JwtService jwt;
    @MockBean private RevokedTokenRepositoryPort revokedTokens;

    @Test
    void catalogoEsPublicoYLosTotalesSonDeCepas() throws Exception {
        var presentacion = new MarketplaceUseCase.PresentacionDetalle("p1", "TAN10", "Bolsa 10 g",
                BigDecimal.TEN, "GRAMOS", new BigDecimal("15000"), 5, null);
        var cepa = new MarketplaceUseCase.ProductoCatalogo("cepa", "Tangerine", null,
                new BigDecimal("15000"), List.of(presentacion));
        when(marketplace.listar(new MarketplaceCatalogFilter("10gr", null, null, null), 1, 2)).thenReturn(new org.springframework.data.domain.PageImpl<>(
                List.of(cepa), org.springframework.data.domain.PageRequest.of(1, 2), 5));
        mvc.perform(get("/api/marketplace/presentaciones").param("buscar", "10gr").param("page", "1").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3)).andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.content[0].cepaId").value("cepa"))
                .andExpect(jsonPath("$.content[0].precioDesde").value(15000))
                .andExpect(jsonPath("$.content[0].presentaciones[0].presentacionId").value("p1"))
                .andExpect(jsonPath("$..costoBasePorUnidad").doesNotExist());
    }

    @Test
    void filtrosDeCategoriaYPrecioSeRecibenSinAutenticacion() throws Exception {
        var filtro = new MarketplaceCatalogFilter("", List.of(1L, 2L), new BigDecimal("50000"), new BigDecimal("100000"));
        when(marketplace.listar(filtro, 0, 20)).thenReturn(org.springframework.data.domain.Page.empty());
        mvc.perform(get("/api/marketplace/presentaciones").param("categoryIds", "1", "2")
                        .param("minPrice", "50000").param("maxPrice", "100000"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        org.mockito.Mockito.verify(marketplace).listar(filtro, 0, 20);
    }

    @Test
    void rechazaFiltrosInvalidosAntesDeConsultar() throws Exception {
        for (String[] parametros : List.of(
                new String[]{"minPrice", "-1"}, new String[]{"maxPrice", "1.001"},
                new String[]{"categoryIds", "0"}, new String[]{"categoryIds", "abc"})) {
            mvc.perform(get("/api/marketplace/presentaciones").param(parametros[0], parametros[1]))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/marketplace/presentaciones").param("minPrice", "20").param("maxPrice", "10"))
                .andExpect(status().isBadRequest());
        org.mockito.Mockito.verifyNoInteractions(marketplace);
    }

    @Test
    void detallePublicoDevuelveNombreCepaYOpcionesSinCostosInternos() throws Exception {
        when(marketplace.obtenerDetalle("cepa", "presentacion")).thenReturn(
                new MarketplaceUseCase.DetalleProducto("cepa", "Tangerine", "presentacion", List.of(
                        new MarketplaceUseCase.PresentacionDetalle("presentacion", "TAN10", "10g",
                                BigDecimal.TEN, "GRAMOS", new BigDecimal("15000"), 5, null))));

        mvc.perform(get("/api/marketplace/cepas/cepa/presentaciones/presentacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Tangerine"))
                .andExpect(jsonPath("$.presentaciones[0].contenidoUnidad").value(10))
                .andExpect(jsonPath("$.presentaciones[0].stockDisponible").value(5))
                .andExpect(jsonPath("$..costoPresentacion").doesNotExist())
                .andExpect(jsonPath("$..proveedorId").doesNotExist());
    }

    @Test
    void cepaIncorrectaResponde404() throws Exception {
        when(marketplace.obtenerDetalle("otra", "presentacion"))
                .thenThrow(new ResourceNotFoundException("La presentacion no pertenece a la cepa indicada"));
        mvc.perform(get("/api/marketplace/cepas/otra/presentaciones/presentacion"))
                .andExpect(status().isNotFound());
    }

    @Test
    void idMalFormadoResponde400() throws Exception {
        when(marketplace.obtenerDetalle("cepa", "invalido"))
                .thenThrow(new IllegalArgumentException("El ID de presentacion debe ser un ObjectId valido"));
        mvc.perform(get("/api/marketplace/cepas/cepa/presentaciones/invalido"))
                .andExpect(status().isBadRequest());
    }
}

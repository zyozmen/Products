package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.domain.exception.CapacidadExcedidaException;
import com.zyozmen.products.domain.exception.MargenNegativoException;
import com.zyozmen.products.domain.model.Container;
import com.zyozmen.products.domain.model.Product;
import com.zyozmen.products.domain.model.LotePaymentState;
import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.port.in.PresentacionUseCase;
import com.zyozmen.products.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PresentacionControllerTest {
    private static final String URL = "/api/v1/lotes/l1/contenedores/c1/presentaciones";
    private static final String VALID = """
            [{"sku":"SAT-5","nombre":"Bolsa 5g","contenidoUnidad":5,"unidadesCreadas":100,
              "precioVenta":45000,"activoMarketplace":true,"confirmacionMargenNegativo":false,
              "fotografia":{"nombre":"5g.jpg","extension":"jpg","filepart":"AQID"}}]
            """;
    @Mock private PresentacionUseCase useCase;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new PresentacionController(useCase))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void recibeArregloYDevuelveLoteConPresentaciones() throws Exception {
        when(useCase.empaquetar(eq("l1"), eq("c1"), anyList())).thenReturn(Lote.builder().id("l1")
                .estadoPago(LotePaymentState.PENDIENTE).estado("PROCESADO")
                .contenedores(List.of(Container.builder().id("c1").tipoCepa("Sativa")
                        .cepaId(Product.desdeNombre("Sativa").id()).build())).build());
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("PROCESADO"))
                .andExpect(jsonPath("$.contenedores[0].presentaciones").isArray())
                .andExpect(jsonPath("$.contenedores[0].cepaId").value(Product.desdeNombre("Sativa").id()));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PresentacionUseCase.CrearPresentacionCommand>> captor = ArgumentCaptor.forClass(List.class);
        verify(useCase).empaquetar(eq("l1"), eq("c1"), captor.capture());
        assertThat(captor.getValue().getFirst().unidadesCreadas()).isEqualTo(100);
        assertThat(captor.getValue().getFirst().precioVenta()).isEqualByComparingTo("45000");
        assertThat(captor.getValue().getFirst().fotografia().getFilepart()).isEqualTo("AQID");
    }

    @Test
    void capacidadDevuelve400ConMensajeExacto() throws Exception {
        String message = "El total empaquetado (1005GRAMOS) supera la capacidad máxima del contenedor (1000GRAMOS)";
        when(useCase.empaquetar(anyString(), anyString(), anyList())).thenThrow(new CapacidadExcedidaException(message));
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void margenNegativoDevuelve422ConMensajeExacto() throws Exception {
        String message = "Atención: El precio de venta (20000) es menor al costo base de la presentación (25000)";
        when(useCase.empaquetar(anyString(), anyString(), anyList())).thenThrow(new MargenNegativoException(message));
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void validaArregloYObjetosAnidadosAntesDelServicio() throws Exception {
        for (String body : List.of("[]", "[null]", "[{}]", "null", "{",
                VALID.replace("\"fotografia\":", "\"fotografiaUrl\":"),
                VALID.replace("\"unidadesCreadas\":100", "\"unidadesCreadas\":2147483648"),
                VALID.replace("\"contenidoUnidad\":5", "\"contenidoUnidad\":0"),
                VALID.replace("\"unidadesCreadas\":100", "\"unidadesCreadas\":-1"),
                VALID.replace("\"precioVenta\":45000", "\"precioVenta\":0.001"),
                VALID.replace("\"extension\":\"jpg\"", "\"extension\":\"svg\""),
                VALID.replace("\"filepart\":\"AQID\"", "\"filepart\":\"\""))) {
            mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(useCase);
    }
}

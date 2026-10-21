package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.domain.model.Contenedor;
import com.zyozmen.products.domain.exception.BusinessRuleException;
import com.zyozmen.products.domain.model.EstadoPagoLote;
import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.model.Proveedor;
import com.zyozmen.products.domain.model.EstadoProveedor;
import com.zyozmen.products.domain.model.TipoTransaccionProveedor;
import com.zyozmen.products.domain.model.TransaccionProveedor;
import com.zyozmen.products.domain.port.in.ProveedorUseCase;
import com.zyozmen.products.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@ExtendWith(MockitoExtension.class)
class ProveedorControllerTest {
    @Mock
    private ProveedorUseCase useCase;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ProveedorController(useCase))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void consultaProveedorConservaContratoYAgregaCantidadesDeLotes() throws Exception {
        var proveedor = Proveedor.builder().id("p1").nombre("Distribuidora Cannabis SAS").nit("900123456")
                .telefono("3001234567").email("proveedor@example.com").estado(EstadoProveedor.ACTIVO)
                .totalDeuda(new BigDecimal("8000000.00")).totalPagado(new BigDecimal("3000000.00"))
                .saldoPendiente(new BigDecimal("5000000.00"))
                .fechaCreacion(Instant.parse("2026-10-21T15:00:00Z")).build();
        when(useCase.obtenerDetalle("p1"))
                .thenReturn(new ProveedorUseCase.DetalleProveedor(proveedor, 3, 2));

        mvc.perform(get("/api/proveedores/p1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("p1"))
                .andExpect(jsonPath("$.nombre").value("Distribuidora Cannabis SAS"))
                .andExpect(jsonPath("$.nit").value("900123456"))
                .andExpect(jsonPath("$.contacto.telefono").value("3001234567"))
                .andExpect(jsonPath("$.contacto.email").value("proveedor@example.com"))
                .andExpect(jsonPath("$.totalDeuda").value(8000000))
                .andExpect(jsonPath("$.totalPagado").value(3000000))
                .andExpect(jsonPath("$.saldoPendiente").value(5000000))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.fechaCreacion").isNotEmpty())
                .andExpect(jsonPath("$.lotesPagados").value(3))
                .andExpect(jsonPath("$.lotesPorPagar").value(2))
                .andExpect(jsonPath("$.proveedor").doesNotExist());
        verify(useCase).obtenerDetalle("p1");
    }

    @Test
    void consultaProveedorSinLotesExponeCantidadesCero() throws Exception {
        var proveedor = Proveedor.builder().id("p1").estado(EstadoProveedor.ACTIVO).build();
        when(useCase.obtenerDetalle("p1"))
                .thenReturn(new ProveedorUseCase.DetalleProveedor(proveedor, 0, 0));

        mvc.perform(get("/api/proveedores/p1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.lotesPagados").value(0))
                .andExpect(jsonPath("$.lotesPorPagar").value(0));
    }

    @Test
    void consultaProveedorInexistenteConserva404() throws Exception {
        when(useCase.obtenerDetalle("otro")).thenThrow(
                new com.zyozmen.products.domain.exception.ResourceNotFoundException("Proveedor no encontrado"));

        mvc.perform(get("/api/proveedores/otro")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Proveedor no encontrado"));
    }

    @Test
    void recibeContenedoresSinCodigoDeClienteYExponeInventario() throws Exception {
        when(useCase.recibirLote(any())).thenReturn(lote());

        mvc.perform(post("/api/lotes").contentType(MediaType.APPLICATION_JSON).content("""
                {"proveedorId":"p1","contenedores":[{"tipoCepa":"Sativa Premium","unidadBase":"gramo",
                "capacidadUnidadesBase":1000,"costoBasePorUnidad":5000}],"abonoInicial":0}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoLote").value("LOT-20261021-ABC"))
                .andExpect(jsonPath("$.estado").value("RECIBIDO"))
                .andExpect(jsonPath("$.contenedores[0].cantidadDisponible").value(1000))
                .andExpect(jsonPath("$.contenedores[0].tipoCepa").value("Sativa Premium"))
                .andExpect(jsonPath("$.presentaciones").doesNotExist());

        var captor = ArgumentCaptor.forClass(ProveedorUseCase.RecibirLoteCommand.class);
        verify(useCase).recibirLote(captor.capture());
        assertThat(captor.getValue().contenedores()).hasSize(1);
        assertThat(captor.getValue().contenedores().getFirst().capacidadUnidadesBase())
                .isEqualByComparingTo("1000");
    }

    @Test
    void rechazaListaVaciaConMensajeDeAceptacion() throws Exception {
        mvc.perform(post("/api/lotes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proveedorId\":\"p1\",\"contenedores\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0]").value(
                        "Debe agregar al menos un contenedor con su capacidad en unidades base para registrar el lote"));
        verifyNoInteractions(useCase);
    }

    @Test
    void validaContenedoresAnidadosYNoAceptaFormatoAnterior() throws Exception {
        for (String cuerpo : List.of(
                "{\"proveedorId\":\"p1\",\"presentaciones\":[]}",
                "{\"proveedorId\":\"p1\",\"contenedores\":[null]}",
                """
                {"proveedorId":"p1","contenedores":[{"tipoCepa":"","unidadBase":"gramo",
                "capacidadUnidadesBase":0,"costoBasePorUnidad":-1}]}
                """,
                """
                {"proveedorId":"p1","contenedores":[{"tipoCepa":"Sativa","unidadBase":"gramo",
                "capacidadUnidadesBase":1.0001,"costoBasePorUnidad":0.001}]}
                """)) {
            mvc.perform(post("/api/lotes").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(useCase);
    }

    @Test
    void consultaLoteConSusContenedores() throws Exception {
        when(useCase.obtenerLote("l1")).thenReturn(lote());
        mvc.perform(get("/api/lotes/l1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.contenedores[0].id").value("c1"));
    }

    @Test
    void abonoExcesivoExponeMensajeExactoComoErrorFinanciero() throws Exception {
        String mensaje = "El abono inicial no puede ser mayor al valor total del lote ($4,000,000 COP)";
        when(useCase.recibirLote(any())).thenThrow(new BusinessRuleException(mensaje));
        mvc.perform(post("/api/lotes").contentType(MediaType.APPLICATION_JSON).content("""
                {"proveedorId":"p1","contenedores":[{"tipoCepa":"Sativa","unidadBase":"gramo",
                "capacidadUnidadesBase":800,"costoBasePorUnidad":5000}],"abonoInicial":5000000}
                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(mensaje));
    }

    @Test
    void consultaHistorialConFechaMontoProveedorYLote() throws Exception {
        when(useCase.listarAbonos("p1", 0, 20)).thenReturn(new PageImpl<>(List.of(TransaccionProveedor.builder()
                .id("t1").proveedorId("p1").loteId("l1").monto(new BigDecimal("1000000.00"))
                .fecha(Instant.parse("2026-10-21T15:00:00Z")).tipo(TipoTransaccionProveedor.ABONO).build())));
        mvc.perform(get("/api/proveedores/p1/abonos")).andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$[0].monto").value(1000000))
                .andExpect(jsonPath("$[0].proveedorId").value("p1"))
                .andExpect(jsonPath("$[0].loteId").value("l1"))
                .andExpect(jsonPath("$[0].fecha").isNotEmpty())
                .andExpect(jsonPath("$[0].tipo").value("ABONO"));
    }

    @Test
    void historialPaginaFueraDeRangoDevuelveArregloVacioYTotalReal() throws Exception {
        when(useCase.listarAbonos("p1", 3, 10))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(3, 10), 21));
        mvc.perform(get("/api/proveedores/p1/abonos").param("page", "3").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "21"))
                .andExpect(jsonPath("$").isEmpty());
        verify(useCase).listarAbonos("p1", 3, 10);
    }

    @Test
    void historialSinMovimientosDevuelveTotalCero() throws Exception {
        when(useCase.listarAbonos("p1", 0, 20)).thenReturn(new PageImpl<>(List.of()));
        mvc.perform(get("/api/proveedores/p1/abonos")).andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void historialRechazaParametrosNoNumericosYPropagaValidacion() throws Exception {
        mvc.perform(get("/api/proveedores/p1/abonos").param("size", "abc"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(useCase);
        when(useCase.listarAbonos("p1", 0, 201)).thenThrow(new IllegalArgumentException("Paginacion invalida"));
        mvc.perform(get("/api/proveedores/p1/abonos").param("size", "201"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Paginacion invalida"));
    }

    @Test
    void buscaPorNombreONitConPaginaYTotalEnEncabezado() throws Exception {
        var proveedor = Proveedor.builder().id("p1").nombre("Distribuidora Cannabis SAS")
                .nit("900123456").estado(EstadoProveedor.ACTIVO).build();
        for (String texto : List.of("distribuidora", "900123")) {
            when(useCase.buscar(texto, 1, 10))
                    .thenReturn(new PageImpl<>(List.of(proveedor), PageRequest.of(1, 10), 11));
            mvc.perform(get("/api/proveedores").param("buscar", texto).param("page", "1").param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-Total-Count", "11"))
                    .andExpect(jsonPath("$[0].id").value("p1"))
                    .andExpect(jsonPath("$[0].nombre").value("Distribuidora Cannabis SAS"))
                    .andExpect(jsonPath("$[0].nit").value("900123456"));
        }
    }

    @Test
    void listadoUsaPaginaCeroYVeinteResultadosPorDefecto() throws Exception {
        when(useCase.buscar(null, 0, 20)).thenReturn(new PageImpl<>(List.of()));

        mvc.perform(get("/api/proveedores"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"))
                .andExpect(jsonPath("$").isEmpty());
        verify(useCase).buscar(null, 0, 20);
    }

    @Test
    void paginacionNoNumericaOInvalidaDevuelve400() throws Exception {
        mvc.perform(get("/api/proveedores").param("page", "abc")).andExpect(status().isBadRequest());
        verifyNoInteractions(useCase);
        when(useCase.buscar(null, -1, 20)).thenThrow(new IllegalArgumentException("Pagina invalida"));
        mvc.perform(get("/api/proveedores").param("page", "-1")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Pagina invalida"));
    }

    private Lote lote() {
        return Lote.builder().id("l1").proveedorId("p1").codigoLote("LOT-20261021-ABC")
                .estado("RECIBIDO").estadoPago(EstadoPagoLote.PENDIENTE)
                .fechaIngreso(Instant.parse("2026-10-21T15:00:00Z"))
                .contenedores(List.of(Contenedor.builder().id("c1").tipoCepa("Sativa Premium")
                        .unidadBase("gramo").capacidadUnidadesBase(new BigDecimal("1000"))
                        .cantidadDisponible(new BigDecimal("1000")).estado("RECIBIDO").build())).build();
    }

    @Test
    void listaLotesConValoresPorDefectoYTotalEnEncabezado() throws Exception {
        when(useCase.listarLotes("p1", null, 0, 20, "fechaIngreso,desc"))
                .thenReturn(new PageImpl<>(List.of(lote())));
        mvc.perform(get("/api/proveedores/p1/lotes")).andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$[0].id").value("l1"))
                .andExpect(jsonPath("$[0].contenedores").isArray());
        verify(useCase).listarLotes("p1", null, 0, 20, "fechaIngreso,desc");
    }

    @Test
    void listaLotesConBusquedaPaginaYOrdenPersonalizado() throws Exception {
        when(useCase.listarLotes("p1", "Bolsa", 1, 5, "costoTotalLote,asc"))
                .thenReturn(new PageImpl<>(List.of(lote()), PageRequest.of(1, 5), 6));
        mvc.perform(get("/api/proveedores/p1/lotes").param("buscar", "Bolsa")
                        .param("page", "1").param("size", "5").param("sort", "costoTotalLote,asc"))
                .andExpect(status().isOk()).andExpect(header().string("X-Total-Count", "6"))
                .andExpect(jsonPath("$[0].codigoLote").value("LOT-20261021-ABC"));
    }

    @Test
    void listaLotesFueraDeRangoConservaTotalReal() throws Exception {
        when(useCase.listarLotes("p1", null, 5, 20, "fechaIngreso,desc"))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(5, 20), 3));
        mvc.perform(get("/api/proveedores/p1/lotes").param("page", "5"))
                .andExpect(status().isOk()).andExpect(header().string("X-Total-Count", "3"))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listaLotesSinCoincidenciasDevuelveTotalCero() throws Exception {
        when(useCase.listarLotes("p1", "inexistente", 0, 20, "fechaIngreso,desc"))
                .thenReturn(new PageImpl<>(List.of()));
        mvc.perform(get("/api/proveedores/p1/lotes").param("buscar", "inexistente"))
                .andExpect(status().isOk()).andExpect(header().string("X-Total-Count", "0"))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listaLotesExpone404YErroresDeParametros() throws Exception {
        mvc.perform(get("/api/proveedores/p1/lotes").param("page", "abc"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(useCase);
        when(useCase.listarLotes("p1", null, 0, 20, "no-valido"))
                .thenThrow(new IllegalArgumentException("Orden invalido"));
        mvc.perform(get("/api/proveedores/p1/lotes").param("sort", "no-valido"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Orden invalido"));
        when(useCase.listarLotes("p1", null, 0, 20, "fechaIngreso,desc"))
                .thenThrow(new com.zyozmen.products.domain.exception.ResourceNotFoundException("Proveedor no encontrado"));
        mvc.perform(get("/api/proveedores/p1/lotes")).andExpect(status().isNotFound());
    }
}

package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.BusinessRuleException;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.EstadoPagoLote;
import com.zyozmen.products.domain.model.EstadoProveedor;
import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.model.Proveedor;
import com.zyozmen.products.domain.model.Cepa;
import com.zyozmen.products.domain.port.in.CepaUseCase;
import com.zyozmen.products.domain.model.TipoTransaccionProveedor;
import com.zyozmen.products.domain.model.TransaccionProveedor;
import com.zyozmen.products.domain.port.in.ProveedorUseCase.CrearProveedorCommand;
import com.zyozmen.products.domain.port.in.ProveedorUseCase.ContenedorCommand;
import com.zyozmen.products.domain.port.in.ProveedorUseCase.RecibirLoteCommand;
import com.zyozmen.products.domain.port.in.ProveedorUseCase.RegistrarPagoCommand;
import com.zyozmen.products.domain.port.out.CodigoInventarioGeneratorPort;
import com.zyozmen.products.domain.port.out.LoteRepositoryPort;
import com.zyozmen.products.domain.port.out.ProveedorRepositoryPort;
import com.zyozmen.products.domain.port.out.TransaccionProveedorRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProveedorServiceTest {

    private static final String PROVEEDOR_ID = "60d5ecb8f1d2c2a4c8e9a010";

    @Mock
    private ProveedorRepositoryPort proveedorRepository;
    @Mock
    private LoteRepositoryPort loteRepository;
    @Mock
    private TransaccionProveedorRepositoryPort transaccionRepository;
    @Mock
    private CodigoInventarioGeneratorPort codigoInventarioGenerator;
    @Mock private CepaUseCase cepas;
    @InjectMocks
    private ProveedorService service;

    private Proveedor proveedor;
    private final AtomicLong siguienteLote = new AtomicLong(1);
    private final AtomicLong siguienteContenedor = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        when(cepas.obtener(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(i -> new Cepa(i.getArgument(0), i.getArgument(0)));
        when(codigoInventarioGenerator.siguienteNumeroLote()).thenAnswer(i -> siguienteLote.getAndIncrement());
        when(codigoInventarioGenerator.siguienteNumeroContenedor())
                .thenAnswer(i -> siguienteContenedor.getAndIncrement());
        proveedor = Proveedor.builder().id(PROVEEDOR_ID).nombre("CafÃ© Colombia").nit("900123456-1")
                .totalDeuda(bd("0.00")).totalPagado(bd("0.00")).saldoPendiente(bd("0.00"))
                .estado(EstadoProveedor.ACTIVO).fechaCreacion(Instant.now()).build();
        when(proveedorRepository.findById(PROVEEDOR_ID)).thenReturn(Optional.of(proveedor));
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(i -> i.getArgument(0));
        when(loteRepository.save(any(Lote.class))).thenAnswer(i -> {
            Lote lote = i.getArgument(0);
            if (lote.getId() == null) {
                lote.setId("lote-1");
            }
            return lote;
        });
        when(transaccionRepository.insert(any(TransaccionProveedor.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void consultaProveedorIncluyeConteosGlobalesSinModificarSaldos() {
        proveedorConDeuda("300000.00", "100000.00");
        when(loteRepository.countPagadosByProveedorId(PROVEEDOR_ID)).thenReturn(3L);
        when(loteRepository.countPorPagarByProveedorId(PROVEEDOR_ID)).thenReturn(2L);

        var detalle = service.obtenerDetalle(PROVEEDOR_ID);

        assertThat(detalle.proveedor()).isSameAs(proveedor);
        assertThat(detalle.lotesPagados()).isEqualTo(3);
        assertThat(detalle.lotesPorPagar()).isEqualTo(2);
        assertThat(proveedor.getSaldoPendiente()).isEqualByComparingTo("200000.00");
        verify(loteRepository).countPagadosByProveedorId(PROVEEDOR_ID);
        verify(loteRepository).countPorPagarByProveedorId(PROVEEDOR_ID);
        verify(proveedorRepository, never()).save(any());
        verify(loteRepository, never()).save(any());
    }

    @Test
    void recibirLoteExigeCepaExistenteYUsaSuNombreReal() {
        when(cepas.obtener("cepa-id")).thenReturn(new Cepa("cepa-id", "Tangerine"));
        var lote = service.recibirLote(new RecibirLoteCommand(PROVEEDOR_ID,
                List.of(new ContenedorCommand("cepa-id", "g", null, bd("100"), bd("1000"))), null));
        assertThat(lote.getContenedores().getFirst().getCepaId()).isEqualTo("cepa-id");
        assertThat(lote.getContenedores().getFirst().getTipoCepa()).isEqualTo("Tangerine");
        when(cepas.obtener("inexistente")).thenThrow(new ResourceNotFoundException("Cepa no encontrada"));
        assertThrows(ResourceNotFoundException.class, () -> service.recibirLote(new RecibirLoteCommand(PROVEEDOR_ID,
                List.of(new ContenedorCommand("inexistente", "g", null, bd("100"), bd("1000"))), null)));
    }

    @Test
    void consultaProveedorSinLotesDevuelveAmbosConteosEnCero() {
        var detalle = service.obtenerDetalle(PROVEEDOR_ID);

        assertThat(detalle.lotesPagados()).isZero();
        assertThat(detalle.lotesPorPagar()).isZero();
    }

    @Test
    void consultaProveedorInexistenteNoCuentaLotes() {
        assertThrows(ResourceNotFoundException.class, () -> service.obtenerDetalle("otro"));
        org.mockito.Mockito.verifyNoInteractions(loteRepository);
    }

    @Test
    void crearInicializaSaldosEnCero() {
        when(proveedorRepository.existsByNit("111")).thenReturn(false);

        Proveedor creado = service.crear(new CrearProveedorCommand("Nuevo", "111", "+573001234567", "a@b.com"));

        assertEquals(bd("0.00"), creado.getTotalDeuda());
        assertEquals(bd("0.00"), creado.getTotalPagado());
        assertEquals(bd("0.00"), creado.getSaldoPendiente());
        assertEquals(EstadoProveedor.ACTIVO, creado.getEstado());
    }

    @Test
    void crearRechazaNitDuplicado() {
        when(proveedorRepository.existsByNit("111")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> service.crear(new CrearProveedorCommand("N", "111", null, null)));
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    void recibirLoteConAbonoCalculaCostoYSaldos() {
        Lote lote = service.recibirLote(comandoLote(bd("100000")));

        assertEquals(bd("300000.00"), lote.getCostoTotalLote());
        assertEquals(bd("100000.00"), lote.getMontoPagadoLote());
        assertEquals(bd("200000.00"), lote.getSaldoPendienteLote());
        assertEquals(EstadoPagoLote.PAGO_PARCIAL, lote.getEstadoPago());
        assertEquals(bd("300000.00"), proveedor.getTotalDeuda());
        assertEquals(bd("100000.00"), proveedor.getTotalPagado());
        assertEquals(bd("200000.00"), proveedor.getSaldoPendiente());

        ArgumentCaptor<TransaccionProveedor> captor = ArgumentCaptor.forClass(TransaccionProveedor.class);
        verify(transaccionRepository, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertEquals(TipoTransaccionProveedor.DEUDA_LOTE, captor.getAllValues().get(0).getTipo());
        assertEquals(TipoTransaccionProveedor.ABONO_INICIAL, captor.getAllValues().get(1).getTipo());
    }

    @Test
    void recibirLoteSinAbonoQuedaPendiente() {
        Lote lote = service.recibirLote(comandoLote(null));

        assertEquals(EstadoPagoLote.PENDIENTE, lote.getEstadoPago());
        assertEquals(bd("300000.00"), proveedor.getSaldoPendiente());
        verify(transaccionRepository).insert(any(TransaccionProveedor.class));
    }

    @Test
    void recibirLoteConAbonoTotalQuedaPagadoTotal() {
        Lote lote = service.recibirLote(comandoLote(bd("300000")));

        assertEquals(EstadoPagoLote.PAGADO_TOTAL, lote.getEstadoPago());
        assertEquals(bd("0.00"), lote.getSaldoPendienteLote());
        assertEquals(bd("0.00"), proveedor.getSaldoPendiente());
    }

    @Test
    void recibirLoteRechazaAbonoMayorAlCosto() {
        assertThrows(BusinessRuleException.class, () -> service.recibirLote(comandoLote(bd("300000.01"))));
        verify(loteRepository, never()).save(any());
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    void recibirLoteRechazaProveedorInactivo() {
        proveedor.setEstado(EstadoProveedor.INACTIVO);

        assertThrows(BusinessRuleException.class, () -> service.recibirLote(comandoLote(null)));
    }

    @Test
    void recibirLoteRechazaProveedorInexistente() {
        when(proveedorRepository.findById("otro")).thenReturn(Optional.empty());
        RecibirLoteCommand command = new RecibirLoteCommand("otro",
                List.of(contenedor("Sativa", "1", "10")), null);

        assertThrows(ResourceNotFoundException.class, () -> service.recibirLote(command));
    }

    @Test
    void pagoAUnLoteActualizaLoteYProveedor() {
        proveedorConDeuda("300000.00", "100000.00");
        Lote lote = loteConSaldo("lote-1", "300000.00", "100000.00", "2026-10-17T11:00:00Z");
        when(loteRepository.findLoteById("lote-1")).thenReturn(Optional.of(lote));

        Proveedor resultado = service.registrarPago(
                new RegistrarPagoCommand(PROVEEDOR_ID, "lote-1", bd("200000"), null, "final"));

        assertEquals(EstadoPagoLote.PAGADO_TOTAL, lote.getEstadoPago());
        assertEquals(bd("0.00"), lote.getSaldoPendienteLote());
        assertEquals(bd("300000.00"), resultado.getTotalPagado());
        assertEquals(bd("0.00"), resultado.getSaldoPendiente());
    }

    @Test
    void pagoSinLoteSeAplicaAlMasAntiguoPrimero() {
        proveedorConDeuda("500000.00", "0.00");
        Lote antiguo = loteConSaldo("l1", "200000.00", "0.00", "2026-10-01T00:00:00Z");
        Lote reciente = loteConSaldo("l2", "300000.00", "0.00", "2026-10-02T00:00:00Z");
        when(loteRepository.findPendientesByProveedorId(PROVEEDOR_ID)).thenReturn(List.of(antiguo, reciente));

        Proveedor resultado = service.registrarPago(
                new RegistrarPagoCommand(PROVEEDOR_ID, null, bd("250000"), TipoTransaccionProveedor.ABONO, null));

        assertEquals(EstadoPagoLote.PAGADO_TOTAL, antiguo.getEstadoPago());
        assertEquals(EstadoPagoLote.PAGO_PARCIAL, reciente.getEstadoPago());
        assertEquals(bd("250000.00"), reciente.getSaldoPendienteLote());
        assertEquals(bd("250000.00"), resultado.getSaldoPendiente());
        ArgumentCaptor<TransaccionProveedor> movimientos = ArgumentCaptor.forClass(TransaccionProveedor.class);
        verify(transaccionRepository, org.mockito.Mockito.times(2)).insert(movimientos.capture());
        assertThat(movimientos.getAllValues()).extracting(TransaccionProveedor::getLoteId)
                .containsExactly("l1", "l2");
        assertThat(movimientos.getAllValues()).extracting(TransaccionProveedor::getMonto)
                .containsExactly(bd("200000"), bd("50000"));
    }

    @Test
    void pagoRechazaMontoMayorAlSaldoDelProveedor() {
        proveedorConDeuda("100000.00", "0.00");

        assertThrows(BusinessRuleException.class, () -> service.registrarPago(
                new RegistrarPagoCommand(PROVEEDOR_ID, null, bd("100000.01"), null, null)));
    }

    @Test
    void pagoRechazaMontoMayorAlSaldoDelLote() {
        proveedorConDeuda("500000.00", "0.00");
        when(loteRepository.findLoteById("lote-1"))
                .thenReturn(Optional.of(loteConSaldo("lote-1", "100000.00", "0.00", "2026-10-01T00:00:00Z")));

        assertThrows(BusinessRuleException.class, () -> service.registrarPago(
                new RegistrarPagoCommand(PROVEEDOR_ID, "lote-1", bd("150000"), null, null)));
    }

    @Test
    void pagoRechazaTiposInvalidosYMontosNoPositivos() {
        assertThrows(BusinessRuleException.class, () -> service.registrarPago(
                new RegistrarPagoCommand(PROVEEDOR_ID, null, bd("10"), TipoTransaccionProveedor.DEUDA_LOTE, null)));
        assertThrows(BusinessRuleException.class, () -> service.registrarPago(
                new RegistrarPagoCommand(PROVEEDOR_ID, null, bd("0"), null, null)));
    }

    @Test
    void pagoRechazaLoteDeOtroProveedor() {
        proveedorConDeuda("100000.00", "0.00");
        Lote ajeno = loteConSaldo("lote-9", "100000.00", "0.00", "2026-10-01T00:00:00Z");
        ajeno.setProveedorId("otro");
        when(loteRepository.findLoteById("lote-9")).thenReturn(Optional.of(ajeno));

        assertThrows(ResourceNotFoundException.class, () -> service.registrarPago(
                new RegistrarPagoCommand(PROVEEDOR_ID, "lote-9", bd("10"), null, null)));
    }

    @Test
    void obtenerLanzaNotFoundSiNoExiste() {
        when(proveedorRepository.findById("x")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtener("x"));
    }

    @Test
    void buscarNormalizaTextoYConservaPaginacion() {
        var pageable = PageRequest.of(1, 10);
        when(proveedorRepository.buscar("900123", pageable))
                .thenReturn(new PageImpl<>(List.of(proveedor), pageable, 11));

        var resultado = service.buscar("  900123  ", 1, 10);

        assertThat(resultado.getContent()).containsExactly(proveedor);
        assertEquals(11, resultado.getTotalElements());
        verify(proveedorRepository).buscar("900123", pageable);
    }

    @Test
    void buscarSinTextoListaProveedores() {
        var pageable = PageRequest.of(0, 20);
        when(proveedorRepository.buscar("", pageable)).thenReturn(new PageImpl<>(List.of(proveedor)));

        assertThat(service.buscar(null, 0, 20).getContent()).containsExactly(proveedor);
        assertThat(service.buscar("   ", 0, 20).getContent()).containsExactly(proveedor);
    }

    @Test
    void buscarRechazaPaginacionInvalida() {
        for (int[] limites : List.of(new int[]{-1, 20}, new int[]{0, 0}, new int[]{0, -1}, new int[]{0, 201})) {
            assertThrows(IllegalArgumentException.class, () -> service.buscar("SAS", limites[0], limites[1]));
        }
        verify(proveedorRepository, never()).buscar(any(), any());
    }

    private RecibirLoteCommand comandoLote(BigDecimal abono) {
        return new RecibirLoteCommand(PROVEEDOR_ID,
                List.of(contenedor("Sativa", "100", "3000")), abono);
    }

    @Test
    void listarLotesOrdenaPorFechaDescendentePorDefectoYValidaProveedor() {
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "fechaIngreso", "id"));
        var lote = loteConSaldo("l1", "100", "0", "2026-10-01T00:00:00Z");
        when(loteRepository.findLotesByProveedorId(PROVEEDOR_ID, "", pageable))
                .thenReturn(new PageImpl<>(List.of(lote), pageable, 1));

        var resultado = service.listarLotes(PROVEEDOR_ID, null, 0, 20, null);

        assertThat(resultado.getContent()).containsExactly(lote);
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        verify(proveedorRepository).findById(PROVEEDOR_ID);
    }

    @Test
    void listarLotesAdmiteTodosLosOrdenamientosYRecortaBusqueda() {
        for (String campo : List.of("fechaIngreso", "codigoLote", "costoTotalLote", "saldoPendienteLote")) {
            for (Sort.Direction direccion : Sort.Direction.values()) {
                var pageable = PageRequest.of(2, 10, Sort.by(direccion, campo, "id"));
                when(loteRepository.findLotesByProveedorId(PROVEEDOR_ID, "Sativa", pageable))
                        .thenReturn(new PageImpl<>(List.of(), pageable, 15));
                var resultado = service.listarLotes(PROVEEDOR_ID, "  Sativa  ", 2, 10,
                        campo + "," + direccion.name());
                assertThat(resultado.getContent()).isEmpty();
                assertThat(resultado.getTotalElements()).isEqualTo(15);
                verify(loteRepository).findLotesByProveedorId(PROVEEDOR_ID, "Sativa", pageable);
            }
        }
    }

    @Test
    void listarLotesRechazaPaginacionYOrdenamientosInvalidos() {
        for (int[] limites : List.of(new int[]{-1, 20}, new int[]{0, 0}, new int[]{0, 201})) {
            assertThrows(IllegalArgumentException.class, () -> service.listarLotes(
                    PROVEEDOR_ID, null, limites[0], limites[1], "fechaIngreso,desc"));
        }
        for (String orden : List.of("", "fechaIngreso", "fechaIngreso,", "fechaIngreso,desc,id",
                "fechaIngreso,invalid", "proveedorId,asc", "contenedores.cepa.nombre,asc")) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.listarLotes(PROVEEDOR_ID, null, 0, 20, orden));
        }
        verify(loteRepository, never()).findLotesByProveedorId(any(), any(), any());
    }

    @Test
    void listarLotesProveedorInexistenteNoConsultaLotes() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.listarLotes("desconocido", null, 0, 20, null));
        verify(loteRepository, never()).findLotesByProveedorId(any(), any(), any());
    }

    @Test
    void listarLotesBusquedaEnBlancoListaTodosLosLotesDelProveedor() {
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "fechaIngreso", "id"));
        when(loteRepository.findLotesByProveedorId(PROVEEDOR_ID, "", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        assertThat(service.listarLotes(PROVEEDOR_ID, "   ", 0, 20, null).getTotalElements()).isZero();
    }

    @Test
    void escenarioUnoRecibeDosContenedoresConInventarioDisponibleYDeudaDeOchoMillones() {
        Instant antes = Instant.now();
        Lote lote = service.recibirLote(new RecibirLoteCommand(PROVEEDOR_ID,
                List.of(contenedor("Sativa Premium", "1000", "5000"),
                        contenedor("Indica Gold", "500", "6000")), BigDecimal.ZERO));

        assertEquals(bd("8000000"), lote.getCostoTotalLote());
        assertEquals(bd("8000000"), proveedor.getTotalDeuda());
        assertEquals(bd("0"), proveedor.getTotalPagado());
        assertEquals(bd("8000000"), proveedor.getSaldoPendiente());
        assertThat(lote.getCodigoLote()).isEqualTo("LOT-000001");
        assertThat(lote.getFechaIngreso()).isBetween(antes, Instant.now());
        assertEquals("RECIBIDO", lote.getEstado());
        assertThat(lote.getContenedores()).hasSize(2).allSatisfy(c -> {
            assertEquals("RECIBIDO", c.getEstado());
            assertEquals(c.getCapacidadUnidadesBase(), c.getCantidadDisponible());
            assertThat(c.getId()).isNotBlank();
        });
        assertThat(lote.getContenedores()).extracting(c -> c.getId()).doesNotHaveDuplicates();
        assertThat(lote.getContenedores()).extracting(c -> c.getCodigoContenedor())
                .containsExactly("CONT-000001", "CONT-000002");
    }

    @Test
    void escenarioDosIncrementaSaldosExistentesConAbonoInicialDeTresMillones() {
        proveedorConDeuda("2000000", "1000000");
        Lote lote = service.recibirLote(new RecibirLoteCommand(PROVEEDOR_ID,
                List.of(contenedor("Sativa", "2000", "5000")), bd("3000000")));

        assertEquals(bd("10000000"), lote.getCostoTotalLote());
        assertEquals(bd("7000000"), lote.getSaldoPendienteLote());
        assertEquals(bd("12000000"), proveedor.getTotalDeuda());
        assertEquals(bd("4000000"), proveedor.getTotalPagado());
        assertEquals(bd("8000000"), proveedor.getSaldoPendiente());
        ArgumentCaptor<TransaccionProveedor> captor = ArgumentCaptor.forClass(TransaccionProveedor.class);
        verify(transaccionRepository, org.mockito.Mockito.times(2)).insert(captor.capture());
        TransaccionProveedor abono = captor.getAllValues().get(1);
        assertEquals(bd("3000000"), abono.getMonto());
        assertEquals(PROVEEDOR_ID, abono.getProveedorId());
        assertEquals(lote.getId(), abono.getLoteId());
        assertEquals(lote.getFechaIngreso(), abono.getFecha());
    }

    @Test
    void escenarioTresRechazaAbonoExcesivoConMensajeExactoSinEscribir() {
        var command = new RecibirLoteCommand(PROVEEDOR_ID,
                List.of(contenedor("Sativa", "800", "5000")), bd("5000000"));
        var error = assertThrows(BusinessRuleException.class, () -> service.recibirLote(command));

        assertEquals("El abono inicial no puede ser mayor al valor total del lote ($4,000,000 COP)",
                error.getMessage());
        verificarSinEscrituras();
    }

    @Test
    void escenarioCuatroRechazaContenedoresVaciosONulosSinEscribir() {
        for (List<ContenedorCommand> contenedores : java.util.Arrays.<List<ContenedorCommand>>asList(List.of(), null)) {
            var error = assertThrows(BusinessRuleException.class, () -> service.recibirLote(
                    new RecibirLoteCommand(PROVEEDOR_ID, contenedores, null)));
            assertEquals("Debe agregar al menos un contenedor con su capacidad en unidades base para registrar el lote",
                    error.getMessage());
        }
        verificarSinEscrituras();
    }

    @Test
    void rechazaCapacidadesCostosYAbonosInvalidosSinRedondearlosSilenciosamente() {
        for (String capacidad : List.of("0", "-1", "1.0001")) {
            assertThrows(BusinessRuleException.class, () -> service.recibirLote(new RecibirLoteCommand(
                    PROVEEDOR_ID, List.of(contenedor("Sativa", capacidad, "5000")), null)));
        }
        for (String costo : List.of("0", "-1", "0.001")) {
            assertThrows(BusinessRuleException.class, () -> service.recibirLote(new RecibirLoteCommand(
                    PROVEEDOR_ID, List.of(contenedor("Sativa", "1", costo)), null)));
        }
        assertThrows(BusinessRuleException.class, () -> service.recibirLote(new RecibirLoteCommand(
                PROVEEDOR_ID, List.of(contenedor(" ", "1", "1")), null)));
        assertThrows(BusinessRuleException.class, () -> service.recibirLote(new RecibirLoteCommand(
                PROVEEDOR_ID, List.of(contenedor("Sativa", "1", "1")), new BigDecimal("-0.001"))));
        verificarSinEscrituras();
    }

    @Test
    void calculaCapacidadFraccionariaYGeneraCodigosDistintos() {
        var command = new RecibirLoteCommand(PROVEEDOR_ID,
                List.of(contenedor("Sativa", "1.125", "5000")), null);
        Lote primero = service.recibirLote(command);
        Lote segundo = service.recibirLote(command);

        assertEquals(bd("5625"), primero.getCostoTotalLote());
        assertThat(primero.getCodigoLote()).isNotEqualTo(segundo.getCodigoLote());
    }

    @Test
    void escenarioCincoRegistraAbonoEnLoteMasAntiguoYConsultaSuMovimiento() {
        proveedorConDeuda("7000000", "0");
        Lote antiguo = loteConSaldo("l1", "2000000", "0", "2026-10-01T00:00:00Z");
        Lote reciente = loteConSaldo("l2", "5000000", "0", "2026-10-15T00:00:00Z");
        when(loteRepository.findPendientesByProveedorId(PROVEEDOR_ID)).thenReturn(List.of(antiguo, reciente));
        Instant antes = Instant.now();

        service.registrarPago(new RegistrarPagoCommand(PROVEEDOR_ID, null, bd("1000000"), null, null));

        ArgumentCaptor<TransaccionProveedor> captor = ArgumentCaptor.forClass(TransaccionProveedor.class);
        verify(transaccionRepository).insert(captor.capture());
        var movimiento = captor.getValue();
        assertEquals("l1", movimiento.getLoteId());
        assertEquals(PROVEEDOR_ID, movimiento.getProveedorId());
        assertEquals(bd("1000000"), movimiento.getMonto());
        assertThat(movimiento.getFecha()).isBetween(antes, Instant.now());
        verify(loteRepository, never()).save(reciente);
        when(transaccionRepository.findAbonosByProveedorId(PROVEEDOR_ID, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(movimiento)));
        assertEquals(List.of(movimiento), service.listarAbonos(PROVEEDOR_ID, 0, 20).getContent());
    }

    @Test
    void rechazaPagoSinLotesSuficientesAntesDeEscribir() {
        proveedorConDeuda("500000", "0");
        when(loteRepository.findPendientesByProveedorId(PROVEEDOR_ID)).thenReturn(List.of(
                loteConSaldo("l1", "100000", "0", "2026-10-01T00:00:00Z")));
        assertThrows(BusinessRuleException.class, () -> service.registrarPago(
                new RegistrarPagoCommand(PROVEEDOR_ID, null, bd("200000"), null, null)));
        verificarSinEscrituras();
    }

    @Test
    void consultarLoteYAbonosValidaExistencia() {
        when(loteRepository.findLoteById("l1")).thenReturn(Optional.of(
                loteConSaldo("l1", "10", "0", "2026-10-01T00:00:00Z")));
        assertEquals("l1", service.obtenerLote("l1").getId());
        assertThrows(ResourceNotFoundException.class, () -> service.obtenerLote("desconocido"));
        assertThrows(ResourceNotFoundException.class, () -> service.listarAbonos("desconocido", 0, 20));
        verify(transaccionRepository, never()).findAbonosByProveedorId(any(), any());
    }

    @Test
    void historialConservaPaginaYTotalDeMovimientos() {
        var pageable = PageRequest.of(2, 10);
        var movimiento = TransaccionProveedor.builder().id("t21").build();
        when(transaccionRepository.findAbonosByProveedorId(PROVEEDOR_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(movimiento), pageable, 21));

        var resultado = service.listarAbonos(PROVEEDOR_ID, 2, 10);

        assertThat(resultado.getContent()).containsExactly(movimiento);
        assertEquals(21, resultado.getTotalElements());
        assertEquals(2, resultado.getNumber());
    }

    @Test
    void historialRechazaPaginacionInvalida() {
        for (int[] limites : List.of(new int[]{-1, 20}, new int[]{0, 0}, new int[]{0, -1}, new int[]{0, 201})) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.listarAbonos(PROVEEDOR_ID, limites[0], limites[1]));
        }
        verify(transaccionRepository, never()).findAbonosByProveedorId(any(), any());
    }

    private ContenedorCommand contenedor(String cepa, String capacidad, String costo) {
        return new ContenedorCommand(cepa, "gramo", "PRES-01",
                new BigDecimal(capacidad), new BigDecimal(costo));
    }

    private void verificarSinEscrituras() {
        verify(loteRepository, never()).save(any());
        verify(proveedorRepository, never()).save(any());
        verify(transaccionRepository, never()).insert(any());
    }

    private void proveedorConDeuda(String deuda, String pagado) {
        proveedor.setTotalDeuda(bd(deuda));
        proveedor.setTotalPagado(bd(pagado));
        proveedor.setSaldoPendiente(bd(deuda).subtract(bd(pagado)));
    }

    private Lote loteConSaldo(String id, String costo, String pagado, String fecha) {
        return Lote.builder().id(id).proveedorId(PROVEEDOR_ID).codigoLote("C-" + id)
                .costoTotalLote(bd(costo)).montoPagadoLote(bd(pagado))
                .saldoPendienteLote(bd(costo).subtract(bd(pagado)))
                .estadoPago(EstadoPagoLote.PENDIENTE).fechaIngreso(Instant.parse(fecha)).build();
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value).setScale(2);
    }
}

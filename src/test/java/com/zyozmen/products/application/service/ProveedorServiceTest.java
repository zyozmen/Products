package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.BusinessRuleException;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.EstadoPagoLote;
import com.zyozmen.products.domain.model.EstadoProveedor;
import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.model.Proveedor;
import com.zyozmen.products.domain.model.TipoTransaccionProveedor;
import com.zyozmen.products.domain.model.TransaccionProveedor;
import com.zyozmen.products.domain.port.in.ProveedorUseCase.CrearProveedorCommand;
import com.zyozmen.products.domain.port.in.ProveedorUseCase.PresentacionCommand;
import com.zyozmen.products.domain.port.in.ProveedorUseCase.RecibirLoteCommand;
import com.zyozmen.products.domain.port.in.ProveedorUseCase.RegistrarPagoCommand;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @InjectMocks
    private ProveedorService service;

    private Proveedor proveedor;

    @BeforeEach
    void setUp() {
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
        when(transaccionRepository.save(any(TransaccionProveedor.class))).thenAnswer(i -> i.getArgument(0));
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
        verify(transaccionRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals(TipoTransaccionProveedor.DEUDA_LOTE, captor.getAllValues().get(0).getTipo());
        assertEquals(TipoTransaccionProveedor.ABONO_INICIAL, captor.getAllValues().get(1).getTipo());
    }

    @Test
    void recibirLoteSinAbonoQuedaPendiente() {
        Lote lote = service.recibirLote(comandoLote(null));

        assertEquals(EstadoPagoLote.PENDIENTE, lote.getEstadoPago());
        assertEquals(bd("300000.00"), proveedor.getSaldoPendiente());
        verify(transaccionRepository).save(any(TransaccionProveedor.class));
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
        RecibirLoteCommand command = new RecibirLoteCommand("otro", "LOT-X",
                List.of(new PresentacionCommand("P", 1, bd("10"))), null);

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

    private RecibirLoteCommand comandoLote(BigDecimal abono) {
        return new RecibirLoteCommand(PROVEEDOR_ID, "LOT-2026-001",
                List.of(new PresentacionCommand("PRES-01", 100, bd("3000"))), abono);
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

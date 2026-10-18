package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.model.Proveedor;
import com.zyozmen.products.domain.port.in.LiquidacionUseCase.EvaluacionLiquidacion;
import com.zyozmen.products.domain.port.in.LiquidacionUseCase.EventoLiquidacionCommand;
import com.zyozmen.products.domain.port.out.LoteRepositoryPort;
import com.zyozmen.products.domain.port.out.ProveedorRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LiquidacionServiceTest {

    @Mock
    private ProveedorRepositoryPort proveedorRepository;
    @Mock
    private LoteRepositoryPort loteRepository;
    @InjectMocks
    private LiquidacionService service;

    private Proveedor proveedor;
    private Lote lote;

    @BeforeEach
    void setUp() {
        proveedor = Proveedor.builder().id("p1").saldoPendiente(bd("200000")).build();
        lote = Lote.builder().id("l1").proveedorId("p1").saldoPendienteLote(bd("200000")).build();
        when(proveedorRepository.findById("p1")).thenReturn(Optional.of(proveedor));
        when(loteRepository.findLoteById("l1")).thenReturn(Optional.of(lote));
    }

    @Test
    void acumulaCuandoHaySaldoPendiente() {
        EvaluacionLiquidacion result = service.evaluarEvento(new EventoLiquidacionCommand("p1", "l1", bd("50000")));

        assertTrue(result.acumulaLiquidacion());
        assertTrue(result.descontarStock());
        assertEquals(bd("50000.00"), result.montoLiquidable());
    }

    @Test
    void limitaElMontoAlSaldoPendienteDelLote() {
        lote.setSaldoPendienteLote(bd("30000"));

        EvaluacionLiquidacion result = service.evaluarEvento(new EventoLiquidacionCommand("p1", "l1", bd("50000")));

        assertEquals(bd("30000"), result.montoLiquidable());
    }

    @Test
    void ignoraMontoSiLoteSinSaldoPeroDescuentaStock() {
        lote.setSaldoPendienteLote(bd("0"));

        EvaluacionLiquidacion result = service.evaluarEvento(new EventoLiquidacionCommand("p1", "l1", bd("50000")));

        assertFalse(result.acumulaLiquidacion());
        assertTrue(result.descontarStock());
        assertEquals(bd("0.00"), result.montoLiquidable());
    }

    @Test
    void ignoraMontoSiProveedorSinSaldo() {
        proveedor.setSaldoPendiente(bd("0"));

        EvaluacionLiquidacion result = service.evaluarEvento(new EventoLiquidacionCommand("p1", "l1", bd("50000")));

        assertFalse(result.acumulaLiquidacion());
        assertTrue(result.descontarStock());
        assertEquals(bd("0.00"), result.montoLiquidable());
    }

    @Test
    void rechazaLoteDeOtroProveedor() {
        lote.setProveedorId("otro");
        EventoLiquidacionCommand command = new EventoLiquidacionCommand("p1", "l1", bd("1"));

        assertThrows(ResourceNotFoundException.class, () -> service.evaluarEvento(command));
    }

    @Test
    void rechazaProveedorInexistente() {
        when(proveedorRepository.findById("zz")).thenReturn(Optional.empty());
        EventoLiquidacionCommand command = new EventoLiquidacionCommand("zz", "l1", bd("1"));

        assertThrows(ResourceNotFoundException.class, () -> service.evaluarEvento(command));
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value).setScale(2);
    }
}

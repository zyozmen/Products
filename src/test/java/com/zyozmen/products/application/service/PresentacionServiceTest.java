package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.BusinessRuleException;
import com.zyozmen.products.domain.exception.CapacidadExcedidaException;
import com.zyozmen.products.domain.exception.MargenNegativoException;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.*;
import com.zyozmen.products.domain.port.in.PresentacionUseCase.CrearPresentacionCommand;
import com.zyozmen.products.domain.port.in.ProductUseCase;
import com.zyozmen.products.domain.port.out.LoteRepositoryPort;
import com.zyozmen.products.domain.port.out.MarketplaceRepositoryPort;
import com.zyozmen.products.domain.port.out.ProveedorRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresentacionServiceTest {
    private static final String LOTE = "650000000000000000000201";
    private static final String CONTENEDOR = "650000000000000000000301";
    @Mock private LoteRepositoryPort lotes;
    @Mock private ProveedorRepositoryPort proveedores;
    @Mock private MarketplaceRepositoryPort marketplace;
    @Mock private ProductUseCase cepas;
    @InjectMocks private PresentacionService service;
    private Lote lote;
    private Provider proveedor;

    @BeforeEach
    void setUp() {
        lote = Lote.builder().id(LOTE).proveedorId("p1").estado("RECIBIDO").version(2L)
                .costoTotalLote(bd("5000000")).montoPagadoLote(bd("1000000"))
                .saldoPendienteLote(bd("4000000")).estadoPago(LotePaymentState.PAGO_PARCIAL)
                .contenedores(List.of(Container.builder().id(CONTENEDOR).tipoCepa("Sativa Premium").cepaId("cepa")
                        .unidadBase("GRAMOS").capacidadUnidadesBase(bd("1000"))
                        .cantidadDisponible(bd("1000")).costoBasePorUnidad(bd("5000")).build())).build();
        proveedor = Provider.builder().id("p1").nombre("AgroSustentable S.A.S")
                .totalDeuda(bd("5000000")).totalPagado(bd("1000000"))
                .saldoPendiente(bd("4000000")).build();
        when(lotes.findLoteById(LOTE)).thenReturn(Optional.of(lote));
        lenient().when(proveedores.findById("p1")).thenReturn(Optional.of(proveedor));
        lenient().when(lotes.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(cepas.obtener("cepa")).thenReturn(new Product("cepa", "Sativa Premium"));
    }

    @Test
    void escenarioUnoEmpaquetaCapacidadExactaCalculaCostoYPublicaStockSinDuplicarDeuda() {
        var resultado = service.empaquetar(LOTE, CONTENEDOR,
                List.of(command("SAT-5", "5", 100, "45000", false),
                        command("SAT-10", "10", 50, "80000", false)));

        assertThat(resultado.getContenedores().getFirst().getCantidadDisponible()).isEqualByComparingTo("0");
        assertThat(resultado.getCostoTotalEmpaquetado()).isEqualByComparingTo("5000000");
        assertThat(resultado.getCostoTotalLote()).isEqualByComparingTo("5000000");
        assertThat(resultado.getSaldoPendienteLote()).isEqualByComparingTo("4000000");
        assertThat(resultado.getEstado()).isEqualTo("PROCESADO");
        assertThat(resultado.getUpdatedAt()).isNotNull();
        var ps = resultado.getContenedores().getFirst().getPresentaciones();
        assertThat(ps).extracting(InventoryPresentation::stockDisponible).containsExactly(100, 50);
        assertThat(ps.getFirst().costoPresentacionCalculado()).isEqualByComparingTo("25000");
        assertThat(ps.getLast().costoPresentacionCalculado()).isEqualByComparingTo("50000");
        var productos = ArgumentCaptor.forClass(ProductMarketplace.class);
        verify(marketplace, times(2)).insert(productos.capture());
        assertThat(productos.getAllValues()).extracting(ProductMarketplace::stockDisponible).containsExactly(100, 50);
        assertThat(productos.getValue().nombreProducto()).isEqualTo("Sativa Premium");
        assertThat(productos.getValue().estado()).isEqualTo("ACTIVO");
        verify(proveedores, never()).save(any());
        assertThat(proveedor.getTotalDeuda()).isEqualByComparingTo("5000000");
    }

    @Test
    void escenarioDosRechazaCapacidadExcedidaConMensajeExactoSinEscribir() {
        assertThatThrownBy(() -> service.empaquetar(LOTE, CONTENEDOR,
                List.of(command("SAT-5", "5", 201, "45000", false))))
                .isInstanceOf(CapacidadExcedidaException.class)
                .hasMessage("El total empaquetado (1005GRAMOS) supera la capacidad máxima del contenedor (1000GRAMOS)");
        sinEscrituras();
    }

    @Test
    void escenarioTresRechazaMargenNegativoSinConfirmacion() {
        assertThatThrownBy(() -> service.empaquetar(LOTE, CONTENEDOR,
                List.of(command("SAT-5", "5", 100, "20000", false))))
                .isInstanceOf(MargenNegativoException.class)
                .hasMessage("Atención: El precio de venta (20000) es menor al costo base de la presentación (25000)");
        sinEscrituras();
    }

    @Test
    void escenarioCuatroPermiteMargenNegativoConfirmadoYConservaSaldoCero() {
        proveedor.setTotalPagado(bd("5000000"));
        proveedor.setSaldoPendiente(bd("0"));
        lote.setMontoPagadoLote(bd("5000000"));
        lote.setSaldoPendienteLote(bd("0"));
        lote.setEstadoPago(LotePaymentState.PAGADO_TOTAL);

        var resultado = service.empaquetar(LOTE, CONTENEDOR,
                List.of(command("SAT-5", "5", 100, "20000", true)));

        assertThat(resultado.getContenedores().getFirst().getPresentaciones().getFirst()
                .confirmacionMargenNegativo()).isTrue();
        assertThat(proveedor.getSaldoPendiente()).isEqualByComparingTo("0");
        assertThat(resultado.getSaldoPendienteLote()).isEqualByComparingTo("0");
        assertThat(resultado.getCostoTotalLote()).isEqualByComparingTo("5000000");
        assertThat(resultado.getCostoTotalEmpaquetado()).isEqualByComparingTo("2500000");
        verify(proveedores, never()).save(any());
    }

    @Test
    void capacidadIncluyeExistentesAunqueSuStockSeHayaVendido() {
        var existente = new InventoryPresentation("650000000000000000000401", "EX", "Existente",
                bd("10"), 50, 0, bd("50000"), bd("80000"), true, false, null);
        lote.setContenedores(List.of(lote.getContenedores().getFirst().toBuilder()
                .presentaciones(List.of(existente)).cantidadDisponible(bd("500")).build()));
        assertThatThrownBy(() -> service.empaquetar(LOTE, CONTENEDOR,
                List.of(command("NUEVA", "10", 51, "80000", false))))
                .isInstanceOf(CapacidadExcedidaException.class);
        sinEscrituras();
    }

    @Test
    void agregaSinReiniciarStockExistenteYAcumulaCostosDeTodosLosContenedores() {
        var existente = new InventoryPresentation("650000000000000000000401", "EX", "Existente",
                bd("10"), 50, 12, bd("50000"), bd("80000"), true, false, null);
        lote.setContenedores(List.of(lote.getContenedores().getFirst(),
                Container.builder().id("otro").presentaciones(List.of(existente)).build()));
        var result = service.empaquetar(LOTE, CONTENEDOR, List.of(command("NUEVA", "5", 10, "30000", false)));
        assertThat(result.getCostoTotalEmpaquetado()).isEqualByComparingTo("2750000");
        assertThat(result.getContenedores().getLast().getPresentaciones().getFirst().stockDisponible()).isEqualTo(12);
        assertThat(result.getContenedores().getFirst().getCantidadDisponible()).isEqualByComparingTo("950");
    }

    @Test
    void agregaAlMismoContenedorSinReiniciarStockVendido() {
        var existente = new InventoryPresentation("650000000000000000000401", "EX", "Existente",
                bd("10"), 50, 12, bd("50000"), bd("80000"), true, false, null);
        lote.setContenedores(List.of(lote.getContenedores().getFirst().toBuilder()
                .presentaciones(List.of(existente)).cantidadDisponible(bd("500")).build()));

        var result = service.empaquetar(LOTE, CONTENEDOR,
                List.of(command("NUEVA", "5", 10, "30000", false)));

        assertThat(result.getContenedores().getFirst().getPresentaciones().getFirst()).isEqualTo(existente);
        assertThat(result.getContenedores().getFirst().getCantidadDisponible()).isEqualByComparingTo("450");
        assertThat(result.getCostoTotalEmpaquetado()).isEqualByComparingTo("2750000");
        verify(marketplace, times(1)).insert(any());
    }

    @Test
    void identificadorDuplicadoEntreSkusDistintosSeRechaza() {
        String id = "650000000000000000000401";
        var primero = new CrearPresentacionCommand(id, "A", "Bolsa A", bd("1"), 1, bd("5000"), true, false, null);
        var segundo = new CrearPresentacionCommand(id, "B", "Bolsa B", bd("1"), 1, bd("5000"), true, false, null);
        assertThatThrownBy(() -> service.empaquetar(LOTE, CONTENEDOR, List.of(primero, segundo)))
                .isInstanceOf(BusinessRuleException.class);
        sinEscrituras();
    }

    @Test
    void rechazaDuplicadosEnPeticionYMarketplace() {
        var duplicado = command("DUP", "5", 1, "30000", false);
        assertThatThrownBy(() -> service.empaquetar(LOTE, CONTENEDOR, List.of(duplicado, duplicado)))
                .isInstanceOf(BusinessRuleException.class);
        when(marketplace.existsBySku("DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.empaquetar(LOTE, CONTENEDOR, List.of(duplicado)))
                .isInstanceOf(BusinessRuleException.class);
        sinEscrituras();
    }

    @Test
    void rechazaIdentificadorExistenteYEntradasInvalidas() {
        assertThatThrownBy(() -> service.empaquetar(LOTE, CONTENEDOR, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.empaquetar(LOTE, "otro",
                List.of(command("X", "1", 1, "5000", false)))).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.empaquetar(LOTE, CONTENEDOR,
                List.of(command("X", "-1", 1, "5000", false)))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.empaquetar(LOTE, CONTENEDOR,
                List.of(command("X", "1", 0, "5000", false)))).isInstanceOf(IllegalArgumentException.class);
        sinEscrituras();
    }

    @Test
    void precioIgualAlCostoNoRequiereConfirmacionYPublicacionInactivaEsExplicita() {
        var c = new CrearPresentacionCommand(null, "X", "Prueba", new BigDecimal("0.125"), 2,
                bd("625"), false, false, null);
        var r = service.empaquetar(LOTE, CONTENEDOR, List.of(c));
        assertThat(r.getCostoTotalEmpaquetado()).isEqualByComparingTo("1250");
        var captor = ArgumentCaptor.forClass(ProductMarketplace.class);
        verify(marketplace).insert(captor.capture());
        assertThat(captor.getValue().estado()).isEqualTo("INACTIVO");
    }

    @Test
    void propagaConflictosDeConcurrenciaYErroresDePublicacionParaRollback() {
        when(lotes.save(any())).thenThrow(new OptimisticLockingFailureException("conflicto"));
        var comando = command("X", "5", 1, "30000", false);
        assertThatThrownBy(() -> service.empaquetar(LOTE, CONTENEDOR, List.of(comando)))
                .isInstanceOf(OptimisticLockingFailureException.class);
        verify(marketplace, never()).insert(any());
    }

    @Test
    void falloEnMarketplaceNoSeOculta() {
        doThrow(new DuplicateKeyException("SKU duplicado concurrente")).when(marketplace).insert(any());
        assertThatThrownBy(() -> service.empaquetar(LOTE, CONTENEDOR,
                List.of(command("X", "5", 1, "30000", false)))).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void conservaFotografiaBase64EnLoteYMarketplace() {
        var foto = new Image("bolsa.png", "PNG", "AQID");
        var comando = new CrearPresentacionCommand(null, "FOTO", "Bolsa", bd("5"), 1,
                bd("30000"), true, false, foto);

        var resultado = service.empaquetar(LOTE, CONTENEDOR, List.of(comando));

        var guardada = resultado.getContenedores().getFirst().getPresentaciones().getFirst().fotografia();
        assertThat(guardada).isEqualTo(new Image("bolsa.png", "png", "AQID"));
        var captor = ArgumentCaptor.forClass(ProductMarketplace.class);
        verify(marketplace).insert(captor.capture());
        assertThat(captor.getValue().fotografia()).isEqualTo(guardada);
    }

    @Test
    void fotografiaInvalidaOExcesivaNoGuardaNingunaPresentacion() {
        for (String filepart : List.of("data:image/png;base64,AQID", "%%%",
                java.util.Base64.getEncoder().encodeToString(new byte[204801]))) {
            var comando = new CrearPresentacionCommand(null, "FOTO", "Bolsa", bd("5"), 1,
                    bd("30000"), true, false, new Image("foto.png", "png", filepart));
            assertThatThrownBy(() -> service.empaquetar(LOTE, CONTENEDOR, List.of(comando)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        sinEscrituras();
    }

    private CrearPresentacionCommand command(String sku, String contenido, int unidades, String precio, boolean confirmar) {
        return new CrearPresentacionCommand(null, sku, "Bolsa " + sku, new BigDecimal(contenido), unidades,
                bd(precio), true, confirmar, null);
    }

    private void sinEscrituras() {
        verify(lotes, never()).save(any());
        verify(marketplace, never()).insert(any());
        verify(proveedores, never()).save(any());
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value).setScale(2);
    }
}

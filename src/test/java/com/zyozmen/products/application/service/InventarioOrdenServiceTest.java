package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.OrderConflictException;
import com.zyozmen.products.domain.exception.OrderProductException;
import com.zyozmen.products.domain.model.*;
import com.zyozmen.products.domain.port.out.LoteRepositoryPort;
import com.zyozmen.products.domain.port.out.MarketplaceRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventarioOrdenServiceTest {
    private final LoteRepositoryPort lotes = mock(LoteRepositoryPort.class);
    private final MarketplaceRepositoryPort marketplace = mock(MarketplaceRepositoryPort.class);
    private final InventarioOrdenService service = new InventarioOrdenService(lotes, marketplace);
    private Lote lote;

    @BeforeEach
    void setUp() {
        var presentacion = new PresentacionInventario("p", "SKU", "10g", BigDecimal.TEN, 10, 8,
                BigDecimal.TEN, BigDecimal.TEN, true, false, null);
        lote = Lote.builder().id("l").proveedorId("proveedor").costoTotalLote(new BigDecimal("100"))
                .saldoPendienteLote(new BigDecimal("100")).contenedores(List.of(
                        Contenedor.builder().id("c").cepaId("cepa").tipoCepa("Tangerine")
                                .cantidadDisponible(new BigDecimal("50"))
                                .presentaciones(List.of(presentacion)).build())).build();
        lenient().when(lotes.findLoteById("l")).thenReturn(Optional.of(lote));
    }

    @Test
    void agrupaItemsRepetidosDescuentaSoloStockEmpaquetadoYConservaDeuda() {
        service.descontar(List.of(item(2), item(3)));
        verify(marketplace).actualizarStock("p", "l", 8, 3);
        var captor = ArgumentCaptor.forClass(Lote.class);
        verify(lotes).save(captor.capture());
        var contenedor = captor.getValue().getContenedores().getFirst();
        assertThat(contenedor.getPresentaciones().getFirst().stockDisponible()).isEqualTo(3);
        assertThat(contenedor.getPresentaciones().getFirst().unidadesCreadas()).isEqualTo(10);
        assertThat(contenedor.getCantidadDisponible()).isEqualByComparingTo("50");
        assertThat(lote.getSaldoPendienteLote()).isEqualByComparingTo("100");
    }

    @Test
    void stockInsuficienteNoEscribe() {
        assertThatThrownBy(() -> service.descontar(List.of(item(9))))
                .isInstanceOf(OrderConflictException.class);
        verifyNoInteractions(marketplace);
        verify(lotes, never()).save(any());
    }

    @Test
    void restitucionNoSuperaUnidadesCreadas() {
        service.restituir(List.of(item(2)));
        assertThat(lote.getContenedores().getFirst().getPresentaciones().getFirst().stockDisponible()).isEqualTo(10);
        assertThatThrownBy(() -> service.restituir(List.of(item(1))))
                .isInstanceOf(OrderConflictException.class);
    }

    @Test
    void rechazaItemsSinTrazabilidadAntesDeTocarRepositorios() {
        assertThatThrownBy(() -> service.descontar(List.of(OrderItem.builder().quantity(1).build())))
                .isInstanceOf(OrderProductException.class);
        verifyNoInteractions(lotes, marketplace);
    }

    @Test
    void conflictoDeMarketplaceNoGuardaLote() {
        doThrow(new OrderConflictException("stock desactualizado")).when(marketplace)
                .actualizarStock("p", "l", 8, 7);
        assertThatThrownBy(() -> service.descontar(List.of(item(1))))
                .isInstanceOf(OrderConflictException.class);
        verify(lotes, never()).save(any());
    }

    @Test
    void compruebaProcedenciaYAcumulacionDeCantidad() {
        var incorrecto = item(1);
        incorrecto.setProductId("otra");
        assertThatThrownBy(() -> service.descontar(List.of(incorrecto)))
                .isInstanceOf(OrderProductException.class);
        assertThatThrownBy(() -> service.descontar(List.of(item(Integer.MAX_VALUE), item(1))))
                .isInstanceOf(OrderProductException.class);
        var original = item(1).getTrazabilidad();
        var otroProveedor = item(1);
        otroProveedor.setTrazabilidad(new TrazabilidadPresentacion("otro", "l", "c", "cepa", "p",
                original.contenidoUnidad(), original.unidadBase(), original.costoBasePorUnidad(),
                original.costoPresentacion()));
        assertThatThrownBy(() -> service.descontar(List.of(otroProveedor)))
                .isInstanceOf(OrderProductException.class);
        verifyNoInteractions(marketplace);
    }

    @Test
    void rechazaItemsVaciosNulosYCantidadesNoPositivas() {
        assertThatThrownBy(() -> service.descontar(null)).isInstanceOf(OrderProductException.class);
        assertThatThrownBy(() -> service.descontar(List.of())).isInstanceOf(OrderProductException.class);
        assertThatThrownBy(() -> service.descontar(java.util.Collections.singletonList(null)))
                .isInstanceOf(OrderProductException.class);
        assertThatThrownBy(() -> service.descontar(List.of(item(0)))).isInstanceOf(OrderProductException.class);
        verifyNoInteractions(lotes, marketplace);
    }

    @Test
    void rechazaTrazabilidadesDistintasParaUnaMismaPresentacion() {
        var otro = item(1);
        otro.setTrazabilidad(new TrazabilidadPresentacion("proveedor", "l", "otro", "cepa", "p",
                BigDecimal.TEN, "GRAMOS", BigDecimal.ONE, BigDecimal.TEN));
        assertThatThrownBy(() -> service.descontar(List.of(item(1), otro))).isInstanceOf(OrderProductException.class);
        verifyNoInteractions(lotes, marketplace);
    }

    @Test
    void presentacionInactivaNoSeVendePeroPermiteRestituir() {
        var p = lote.getContenedores().getFirst().getPresentaciones().getFirst();
        lote.setContenedores(List.of(lote.getContenedores().getFirst().toBuilder().presentaciones(List.of(new PresentacionInventario(
                p.presentacionId(), p.sku(), p.nombre(), p.contenidoUnidad(), p.unidadesCreadas(), p.stockDisponible(),
                p.costoPresentacionCalculado(), p.precioVenta(), false, p.confirmacionMargenNegativo(), p.fotografia())))
                .build()));
        assertThatThrownBy(() -> service.descontar(List.of(item(1)))).isInstanceOf(OrderConflictException.class);
        service.restituir(List.of(item(1)));
        verify(marketplace).actualizarStock("p", "l", 8, 9);
        assertThat(lote.getContenedores().getFirst().getPresentaciones().getFirst().activoMarketplace()).isFalse();
    }

    @Test
    void faltaDeLoteContenedorCepaOPresentacionSeNotificaSinEscrituras() {
        when(lotes.findLoteById("l")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.descontar(List.of(item(1)))).isInstanceOf(OrderProductException.class);
        when(lotes.findLoteById("l")).thenReturn(Optional.of(lote));
        var c = lote.getContenedores().getFirst();
        lote.setContenedores(List.of(c.toBuilder().id("otro").build()));
        assertThatThrownBy(() -> service.descontar(List.of(item(1)))).isInstanceOf(OrderProductException.class);
        lote.setContenedores(List.of(c.toBuilder().cepaId("otra").build()));
        assertThatThrownBy(() -> service.descontar(List.of(item(1)))).isInstanceOf(OrderProductException.class);
        lote.setContenedores(List.of(c.toBuilder().presentaciones(List.of()).build()));
        assertThatThrownBy(() -> service.descontar(List.of(item(1)))).isInstanceOf(OrderProductException.class);
        lote.setContenedores(List.of(c.toBuilder().presentaciones(null).build()));
        assertThatThrownBy(() -> service.descontar(List.of(item(1)))).isInstanceOf(OrderProductException.class);
        lote.setContenedores(null);
        assertThatThrownBy(() -> service.descontar(List.of(item(1)))).isInstanceOf(OrderProductException.class);
        verifyNoInteractions(marketplace);
        verify(lotes, never()).save(any());
    }

    private OrderItem item(int cantidad) {
        return OrderItem.builder().productId("p").quantity(cantidad).trazabilidad(new TrazabilidadPresentacion(
                "proveedor", "l", "c", "cepa", "p", BigDecimal.TEN, "GRAMOS", BigDecimal.ONE, BigDecimal.TEN)).build();
    }
}

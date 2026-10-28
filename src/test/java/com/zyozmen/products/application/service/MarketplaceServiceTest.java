package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.OrderProductException;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.*;
import com.zyozmen.products.domain.port.out.LoteRepositoryPort;
import com.zyozmen.products.domain.port.out.MarketplaceRepositoryPort;
import com.zyozmen.products.domain.port.in.ProductUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketplaceServiceTest {
    @Mock private MarketplaceRepositoryPort marketplace;
    @Mock private LoteRepositoryPort lotes;
    @Mock private ProductUseCase cepas;
    @InjectMocks private MarketplaceService service;

    @BeforeEach
    void cepaPersistida() {
        lenient().when(cepas.obtener(Product.desdeNombre("Tangerine").id()))
                .thenReturn(new Product(Product.desdeNombre("Tangerine").id(), "Tangerine"));
    }

    @Test
    void delegaFiltrosCombinadosYPaginaEnElRepositorio() {
        var filtro = new MarketplaceCatalogFilter("10gr", List.of(7L), BigDecimal.ZERO, new BigDecimal("50000"));
        var pageable = org.springframework.data.domain.PageRequest.of(1, 20);
        when(marketplace.buscarCatalogo(filtro, pageable)).thenReturn(org.springframework.data.domain.Page.empty(pageable));
        assertThat(service.listar(filtro, 1, 20).getNumber()).isEqualTo(1);
        verify(marketplace).buscarCatalogo(filtro, pageable);
        assertThatThrownBy(() -> service.listar(filtro, -1, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listar(filtro, 0, 101)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resuelveCostoYProcedenciaDesdeElLoteSinConfiarEnElNombreComercial() {
        var producto = producto("p1", "l1", "Tangerine");
        when(marketplace.findByPresentacionId("p1")).thenReturn(Optional.of(producto));
        when(lotes.findLoteById("l1")).thenReturn(Optional.of(lote("l1", "c1", "p1", "Tangerine")));

        var venta = service.resolverPresentacion("p1");

        assertThat(venta.nombre()).isEqualTo("Tangerine");
        assertThat(venta.precioVenta()).isEqualByComparingTo("15000");
        assertThat(venta.trazabilidad().costoPresentacion()).isEqualByComparingTo("10000");
        assertThat(venta.trazabilidad().contenidoUnidad()).isEqualByComparingTo("10");
        assertThat(venta.trazabilidad().proveedorId()).isEqualTo("proveedor-l1");
        assertThat(venta.trazabilidad().contenedorId()).isEqualTo("c1");
        assertThat(venta.trazabilidad().cepaId()).isEqualTo(Product.desdeNombre("tangerine").id());
        verify(lotes, never()).save(any());
    }

    @Test
    void mismaCepaAgrupaPresentacionesDeDistintosLotesYProveedores() {
        var p1 = producto("p1", "l1", "Tangerine");
        var p2 = producto("p2", "l2", " tangerine ");
        when(marketplace.findByPresentacionId("p1")).thenReturn(Optional.of(p1));
        when(marketplace.findDisponiblesByCepaId(Product.desdeNombre("Tangerine").id())).thenReturn(List.of(p2, p1));
        when(lotes.findLoteById("l1")).thenReturn(Optional.of(lote("l1", "c1", "p1", "Tangerine")));
        when(lotes.findLoteById("l2")).thenReturn(Optional.of(lote("l2", "c2", "p2", "TANGERINE")));

        var detalle = service.obtenerDetalle(Product.desdeNombre("Tangerine").id(), "p1");

        assertThat(detalle.nombre()).isEqualTo("Tangerine");
        assertThat(detalle.presentacionSeleccionadaId()).isEqualTo("p1");
        assertThat(detalle.presentaciones()).extracting(p -> p.presentacionId()).containsExactly("p1", "p2");
        assertThat(detalle.presentaciones()).allSatisfy(p -> {
            assertThat(p.contenidoUnidad()).isEqualByComparingTo("10");
            assertThat(p.stockDisponible()).isEqualTo(5);
        });
    }

    @Test
    void rechazaPresentacionDeOtraCepa() {
        when(marketplace.findByPresentacionId("p1"))
                .thenReturn(Optional.of(producto("p1", "l1", "Tangerine")));

        assertThatThrownBy(() -> service.obtenerDetalle(Product.desdeNombre("Otra").id(), "p1"))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(lotes);
    }

    @Test
    void rechazaPresentacionSinProcedencia() {
        when(marketplace.findByPresentacionId("p1"))
                .thenReturn(Optional.of(producto("p1", "l1", "Tangerine")));
        when(lotes.findLoteById("l1")).thenReturn(Optional.of(lote("l1", "c1", "distinta", "Tangerine")));

        assertThatThrownBy(() -> service.resolverPresentacion("p1"))
                .isInstanceOf(OrderProductException.class).hasMessageContaining("no pertenece");
    }

    @Test
    void rechazaPresentacionInactivaOInexistente() {
        when(marketplace.findByPresentacionId("p1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resolverPresentacion("p1"))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(lotes);
    }

    @Test
    void rechazaPublicacionInactivaAunqueExistaElLote() {
        var p = producto("p1", "l1", "Tangerine");
        when(marketplace.findByPresentacionId("p1")).thenReturn(Optional.of(new ProductMarketplace(
                p.presentacionId(), p.loteId(), p.proveedorNombre(), p.nombreProducto(), p.cepaNombre(),
                p.sku(), p.precioVenta(), p.stockDisponible(), p.fotografia(), "INACTIVO")));

        assertThatThrownBy(() -> service.resolverPresentacion("p1")).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(lotes);
    }

    @Test
    void cambiarCostoDelContenedorNoModificaSnapshotAnterior() {
        var lote = lote("l1", "c1", "p1", "Tangerine");
        when(marketplace.findByPresentacionId("p1"))
                .thenReturn(Optional.of(producto("p1", "l1", "Tangerine")));
        when(lotes.findLoteById("l1")).thenReturn(Optional.of(lote));
        var anterior = service.resolverPresentacion("p1").trazabilidad();
        lote.setContenedores(List.of(lote.getContenedores().getFirst().toBuilder()
                .costoBasePorUnidad(new BigDecimal("2000")).build()));

        assertThat(service.resolverPresentacion("p1").trazabilidad().costoPresentacion())
                .isEqualByComparingTo("20000");
        assertThat(anterior.costoPresentacion()).isEqualByComparingTo("10000");
    }

    @Test
    void identidadDeCepaComparteMayusculasYEspaciosPeroNoNombresDistintos() {
        assertThat(Product.desdeNombre("  Tangerine   Dream ").id())
                .isEqualTo(Product.desdeNombre("tangerine dream").id())
                .isNotEqualTo(Product.desdeNombre("tangerine").id());
        assertThatThrownBy(() -> Product.desdeNombre(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    private ProductMarketplace producto(String id, String lote, String cepa) {
        return new ProductMarketplace(id, lote, "Proveedor", "Nombre legado", cepa, id,
                new BigDecimal("15000"), 5, null, "ACTIVO", Product.desdeNombre(cepa).id(),
                "Bolsa 10g", BigDecimal.TEN, "GRAMOS");
    }

    private Lote lote(String id, String contenedor, String presentacion, String cepa) {
        var p = new InventoryPresentation(presentacion, presentacion, "Bolsa 10g", BigDecimal.TEN,
                5, 5, new BigDecimal("10000"), new BigDecimal("15000"), true, false, null);
        return Lote.builder().id(id).proveedorId("proveedor-" + id).contenedores(List.of(
                Container.builder().id(contenedor).tipoCepa(cepa).cepaId(Product.desdeNombre(cepa).id()).unidadBase("GRAMOS")
                        .costoBasePorUnidad(new BigDecimal("1000")).presentaciones(List.of(p)).build())).build();
    }
}

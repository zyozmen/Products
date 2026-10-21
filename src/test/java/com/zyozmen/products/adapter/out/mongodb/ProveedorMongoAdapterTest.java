package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.LoteDocument;
import com.zyozmen.products.adapter.out.mongodb.document.TransaccionProveedorDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProveedorDocument;
import com.zyozmen.products.adapter.out.mongodb.mapper.ProveedorMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.LoteMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.repository.ProveedorMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.repository.TransaccionProveedorMongoRepository;
import com.zyozmen.products.domain.model.Contenedor;
import com.zyozmen.products.domain.model.EstadoPagoLote;
import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.model.TipoTransaccionProveedor;
import com.zyozmen.products.domain.model.TransaccionProveedor;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProveedorMongoAdapterTest {
    private static final String PROVEEDOR_ID = "60d5ecb8f1d2c2a4c8e9a010";
    @Mock
    private ProveedorMongoRepository proveedores;
    @Mock
    private LoteMongoRepository lotes;
    @Mock
    private TransaccionProveedorMongoRepository movimientos;
    private final ProveedorMongoMapper mapper = new ProveedorMongoMapper();
    private ProveedorMongoAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ProveedorMongoAdapter(proveedores, lotes, movimientos, mapper);
    }

    @Test
    void persisteYRecuperaInventarioConTodosLosCamposDelContenedor() {
        var contenedor = Contenedor.builder().id("c1").tipoCepa("Sativa").unidadBase("gramo")
                .presentacionId("pres1").capacidadUnidadesBase(new BigDecimal("1.125"))
                .cantidadDisponible(new BigDecimal("1.125")).costoBasePorUnidad(new BigDecimal("5000.00"))
                .subtotal(new BigDecimal("5625.00")).estado("RECIBIDO").build();
        var lote = Lote.builder().id("l1").proveedorId(PROVEEDOR_ID).codigoLote("LOT-1")
                .estadoPago(EstadoPagoLote.PENDIENTE).estado("RECIBIDO").fechaIngreso(Instant.now())
                .contenedores(List.of(contenedor)).build();
        when(lotes.save(any(LoteDocument.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(adapter.save(lote)).isEqualTo(lote);
    }

    @Test
    void pagosEnLotesHistoricosConservanPresentacionesSinInventarContenedores() {
        var documento = LoteDocument.builder().id("l1").estadoPago("PENDIENTE")
                .presentaciones(List.of(LoteDocument.PresentacionDocument.builder().presentacionId("pres1")
                        .cantidadUnidades(100).costoUnitario(BigDecimal.TEN)
                        .subtotal(new BigDecimal("1000")).build())).build();

        var dominio = mapper.toDomain(documento);
        var guardado = mapper.toDocument(dominio);

        assertThat(dominio.getContenedores()).isEmpty();
        assertThat(guardado.getPresentaciones()).isEqualTo(documento.getPresentaciones());
        assertThat(guardado.getEstado()).isNull();
    }

    @Test
    void insertaMovimientosPeroNuncaSobrescribeUnRegistroExistente() {
        var nuevo = TransaccionProveedor.builder().proveedorId(PROVEEDOR_ID).loteId("l1")
                .tipo(TipoTransaccionProveedor.ABONO).monto(BigDecimal.TEN).fecha(Instant.now()).build();
        when(movimientos.insert(any(TransaccionProveedorDocument.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(adapter.insert(nuevo)).isEqualTo(nuevo);
        verify(movimientos, never()).save(any(TransaccionProveedorDocument.class));
        nuevo.setId("existente");
        assertThrows(IllegalArgumentException.class, () -> adapter.insert(nuevo));
    }

    @Test
    void consultaMovimientosPorProveedorConIdentificadorMongoYConservaOrden() {
        var inicial = TransaccionProveedorDocument.builder().id("t2").proveedorId(PROVEEDOR_ID)
                .loteId("l1").tipo("ABONO_INICIAL").monto(BigDecimal.TEN).fecha(Instant.now()).build();
        var posterior = TransaccionProveedorDocument.builder().id("t1").proveedorId(PROVEEDOR_ID)
                .loteId("l2").tipo("ABONO").monto(BigDecimal.ONE).fecha(Instant.now()).build();
        var pageable = PageRequest.of(1, 2);
        when(movimientos.findAbonos(new ObjectId(PROVEEDOR_ID), pageable))
                .thenReturn(new PageImpl<>(List.of(posterior, inicial), pageable, 5));

        var resultado = adapter.findAbonosByProveedorId(PROVEEDOR_ID, pageable);
        assertThat(resultado.getContent())
                .extracting(TransaccionProveedor::getId).containsExactly("t1", "t2");
        assertThat(resultado.getTotalElements()).isEqualTo(5);
        assertThat(resultado.getNumber()).isEqualTo(1);
        verify(movimientos).findAbonos(new ObjectId(PROVEEDOR_ID), pageable);
    }

    @Test
    void buscarUsaCoincidenciaLiteralYOrdenEstable() {
        var pagina = PageRequest.of(2, 10, Sort.by("nombre", "id"));
        var documento = ProveedorDocument.builder().id(PROVEEDOR_ID)
                .nombre("Distribuidora SAS").nit("900123").estado("ACTIVO").build();
        String texto = "SAS.*\\E";
        when(proveedores.buscarPorNombreONit(Pattern.quote(texto), pagina))
                .thenReturn(new PageImpl<>(List.of(documento), pagina, 21));

        var resultado = adapter.buscar(texto, PageRequest.of(2, 10));

        assertThat(resultado.getContent()).extracting(p -> p.getId()).containsExactly(PROVEEDOR_ID);
        assertThat(resultado.getTotalElements()).isEqualTo(21);
        verify(proveedores).buscarPorNombreONit(Pattern.quote(texto), pagina);
        assertThat(Pattern.compile(Pattern.quote(texto), Pattern.CASE_INSENSITIVE)
                .matcher("Distribuidora sas.*\\E Colombia").find()).isTrue();
        assertThat(Pattern.compile(Pattern.quote(texto), Pattern.CASE_INSENSITIVE)
                .matcher("Distribuidora SAS Colombia").find()).isFalse();
    }

    @Test
    void buscarVacioListaSinFiltroYRespetaPagina() {
        var pagina = PageRequest.of(0, 20, Sort.by("nombre", "id"));
        when(proveedores.findAll(pagina)).thenReturn(new PageImpl<>(List.of(), pagina, 0));

        assertThat(adapter.buscar("", PageRequest.of(0, 20)).getContent()).isEmpty();
        verify(proveedores).findAll(pagina);
        verify(proveedores, never()).buscarPorNombreONit(any(), any(Pageable.class));
    }
}

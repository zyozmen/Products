package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.LoteDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProductMarketplaceDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProviderDocument;
import com.zyozmen.products.adapter.out.mongodb.mapper.ProviderMongoMapper;
import com.zyozmen.products.domain.model.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Hu03SchemaMappingTest {
    private static final String LOTE = "650000000000000000000201";
    private static final String PROVEEDOR = "650000000000000000000101";
    private static final String CONTENEDOR = "650000000000000000000301";
    private static final String PRESENTACION = "650000000000000000000401";

    private MappingMongoConverter converter() throws Exception {
        var context = new MongoMappingContext();
        var conversions = org.springframework.data.mongodb.core.convert.MongoCustomConversions.create(adapter -> {
            adapter.registerConverter(new org.springframework.core.convert.converter.Converter<BigDecimal, org.bson.types.Decimal128>() {
                public org.bson.types.Decimal128 convert(BigDecimal source) {
                    return new org.bson.types.Decimal128(source);
                }
            });
            adapter.registerConverter(new org.springframework.core.convert.converter.Converter<org.bson.types.Decimal128, BigDecimal>() {
                public BigDecimal convert(org.bson.types.Decimal128 source) {
                    return source.bigDecimalValue();
                }
            });
        });
        context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        context.afterPropertiesSet();
        var converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
        return converter;
    }

    @Test
    void escribeEsquemaAnidadoConObjectIdsYDecimal128YConservaVersion() throws Exception {
        var mapper = new ProviderMongoMapper();
        var p = new InventoryPresentation(PRESENTACION, "SAT-5", "Bolsa 5g", new BigDecimal("5"),
                100, 100, new BigDecimal("25000"), new BigDecimal("45000"), true, false,
                new Image("foto.png", "png", "AQID"));
        var lote = Lote.builder().id(LOTE).proveedorId(PROVEEDOR).codigoLote("LOT-1")
                .costoTotalLote(new BigDecimal("5000000")).montoPagadoLote(new BigDecimal("0.00"))
                .saldoPendienteLote(new BigDecimal("5000000")).estadoPago(LotePaymentState.PENDIENTE)
                .estado("RECIBIDO").fechaIngreso(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)).version(3L)
                .contenedores(List.of(Container.builder().id(CONTENEDOR).codigoContenedor("CONT-1")
                        .tipoCepa("Sativa").unidadBase("GRAMOS").capacidadUnidadesBase(new BigDecimal("1000"))
                        .costoBasePorUnidad(new BigDecimal("5000")).cantidadDisponible(new BigDecimal("500"))
                        .subtotal(new BigDecimal("5000000")).presentaciones(List.of(p)).build())).build();
        var raw = new Document();
        var converter = converter();
        converter.write(mapper.toDocument(lote), raw);

        assertThat(raw.get("proveedorId")).isEqualTo(new ObjectId(PROVEEDOR));
        assertThat(raw).containsKeys("fechaRecepcion", "costoTotalCalculado", "version")
                .doesNotContainKeys("fechaIngreso", "costoTotalLote");
        assertThat(raw.get("costoTotalCalculado")).isInstanceOf(org.bson.types.Decimal128.class);
        assertThat(raw.getString("estado")).isEqualTo("EN_RECEPCION");
        var c = raw.getList("contenedores", Document.class).getFirst();
        assertThat(c.get("contenedorId")).isEqualTo(new ObjectId(CONTENEDOR));
        assertThat(c).containsKeys("capacidadTotal", "unidadMedida", "cepa", "presentaciones")
                .doesNotContainKeys("_id", "id", "tipoCepa", "capacidadUnidadesBase", "costoBasePorUnidad");
        assertThat(c.get("cepa", Document.class).getString("nombre")).isEqualTo("Sativa");
        assertThat(c.getList("presentaciones", Document.class).getFirst().get("presentacionId"))
                .isEqualTo(new ObjectId(PRESENTACION));
        var fotografia = c.getList("presentaciones", Document.class).getFirst().get("fotografia", Document.class);
        assertThat(fotografia.getString("filepart")).isEqualTo("AQID");
        assertThat(fotografia.getString("extension")).isEqualTo("png");
        assertThat(fotografia.getString("nombre")).isEqualTo("foto.png");
        assertThat(mapper.toDomain(converter.read(LoteDocument.class, raw))).isEqualTo(lote);
    }

    @Test
    void proveedorUsaFinancieroYContactoPlanoSinCambiarRespuestaDeDominio() throws Exception {
        var proveedor = Provider.builder().id(PROVEEDOR).nombre("Proveedor").nit("900123")
                .telefono("123").email("a@example.com").estado(ProviderState.ACTIVO)
                .totalDeuda(BigDecimal.TEN).totalPagado(BigDecimal.ONE).saldoPendiente(new BigDecimal("9"))
                .version(1L).build();
        var mapper = new ProviderMongoMapper();
        var raw = new Document();
        var converter = converter();
        converter.write(mapper.toDocument(proveedor), raw);
        assertThat(raw).containsKeys("financiero", "telefono", "correo")
                .doesNotContainKeys("contacto", "totalDeuda", "totalPagado", "saldoPendiente");
        assertThat(raw.get("financiero", Document.class).get("totalDeuda"))
                .isInstanceOf(org.bson.types.Decimal128.class);
        assertThat(mapper.toDomain(converter.read(ProviderDocument.class, raw))).isEqualTo(proveedor);
    }

    @Test
    void marketplaceUsaObjectIdsYCamposDelContrato() throws Exception {
        var raw = new Document();
        converter().write(new ProductMarketplaceDocument(null, PRESENTACION, LOTE, "Proveedor",
                "Sativa - Bolsa", "Sativa", "SAT-5", new BigDecimal("45000"), 100, null, "ACTIVO"), raw);
        assertThat(raw.get("presentacionId")).isEqualTo(new ObjectId(PRESENTACION));
        assertThat(raw.get("loteId")).isEqualTo(new ObjectId(LOTE));
        assertThat(raw.getInteger("stockDisponible")).isEqualTo(100);
    }
}

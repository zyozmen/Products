package com.zyozmen.products.adapter.out.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.zyozmen.products.adapter.out.mongodb.document.CepaDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ImagenDocument;
import com.zyozmen.products.adapter.out.mongodb.repository.CepaMongoRepository;
import com.zyozmen.products.application.service.CepaService;
import com.zyozmen.products.domain.model.Imagen;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "HU05_TEST_MONGO_URI", matches = ".+")
class MarketplaceCatalogoIntegrationTest {
    private MongoClient client;
    private MongoTemplate template;
    private MarketplaceCatalogoQuery query;

    @BeforeEach
    void setUp() throws Exception {
        client = MongoClients.create(System.getenv("HU05_TEST_MONGO_URI"));
        var factory = new SimpleMongoClientDatabaseFactory(client, "hu05_test_" + UUID.randomUUID().toString().replace("-", ""));
        var conversions = MongoCustomConversions.create(adapter -> {
            adapter.registerConverter(new Converter<BigDecimal, Decimal128>() {
                public Decimal128 convert(BigDecimal value) { return new Decimal128(value); }
            });
            adapter.registerConverter(new Converter<Decimal128, BigDecimal>() {
                public BigDecimal convert(Decimal128 value) { return value.bigDecimalValue(); }
            });
        });
        var context = new MongoMappingContext();
        context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        context.afterPropertiesSet();
        var converter = new MappingMongoConverter(new DefaultDbRefResolver(factory), context);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
        template = new MongoTemplate(factory, converter);
        new MarketplaceIndexInitializer(template).run(null);
        query = new MarketplaceCatalogoQuery(template);
        template.insert(new CepaDocument("a", "Tangerine", "tangerine",
                new ImagenDocument("cepa.webp", "webp", "AQID"), null, null, null));
        template.insert(new CepaDocument("b", "Otra", "otra", null, null, null, null));
        presentacion("a", "Bolsa chica", 5, 8000, 12, "ACTIVO");
        presentacion("a", "Bolsa grande", 10, 15000, 7, "ACTIVO");
        presentacion("a", "Agotada", 1, 1, 0, "ACTIVO");
        presentacion("a", "Oculta", 2, 2, 10, "INACTIVO");
        presentacion("b", "Envase especial", 10, 20000, 4, "ACTIVO");
    }

    @AfterEach
    void tearDown() {
        if (template != null) {
            template.getDb().drop();
        }
        if (client != null) {
            client.close();
        }
    }

    @Test
    void paginacionCuentaCepasYConservaTodasLasOpcionesDisponibles() {
        var page = query.buscar("", PageRequest.of(1, 1));
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        var tangerine = page.getContent().getFirst();
        assertThat(tangerine.cepaId()).isEqualTo("a");
        assertThat(tangerine.precioDesde()).isEqualByComparingTo("8000");
        assertThat(tangerine.fotografia().getFilepart()).isEqualTo("AQID");
        assertThat(tangerine.presentaciones()).hasSize(2).allSatisfy(p -> {
            assertThat(p.stockDisponible()).isPositive();
            assertThat(p.fotografia().getFilepart()).isEqualTo("AQID");
            assertThat(p.presentacionId()).hasSize(24);
        });
        assertThat(query.buscar("", PageRequest.of(10, 1)).getContent()).isEmpty();
    }

    @Test
    void busquedaPorMedidaUsaContenidoNoSoloNombreYDevuelveOpcionesNoCoincidentes() {
        for (String texto : List.of("10gr", "10 g", "10 gramos", "10 GR", "10,000gr")) {
            var page = query.buscar(texto, PageRequest.of(0, 20));
            assertThat(page.getTotalElements()).as(texto).isEqualTo(2);
            var cepa = page.getContent().stream().filter(c -> c.cepaId().equals("a")).findFirst().orElseThrow();
            assertThat(cepa.presentaciones()).hasSize(2);
            assertThat(cepa.precioDesde()).isEqualByComparingTo("8000");
        }
        assertThat(query.buscar("1gr", PageRequest.of(0, 20)).getContent()).isEmpty();
    }

    @Test
    void buscaNombreDeCepaYPresentacionSinInterpretarExpresionesDelUsuario() {
        assertThat(query.buscar("TANGER", PageRequest.of(0, 20)).getTotalElements()).isEqualTo(1);
        var porPresentacion = query.buscar("GRANDE", PageRequest.of(0, 20));
        assertThat(porPresentacion.getContent().getFirst().presentaciones()).hasSize(2);
        assertThat(query.buscar(".*", PageRequest.of(0, 20)).getTotalElements()).isZero();
        assertThat(query.buscar("$cepa", PageRequest.of(0, 20)).getTotalElements()).isZero();
        assertThat(query.buscar("oculta", PageRequest.of(0, 20)).getTotalElements()).isZero();
        assertThat(query.buscar("otra", PageRequest.of(0, 20)).getContent().getFirst().fotografia()).isNull();
    }

    @Test
    void renombradoPersisteIdentidadYBusquedaUsaNombreActual() {
        var repository = new MongoRepositoryFactory(template).getRepository(CepaMongoRepository.class);
        var service = new CepaService(new CepaMongoAdapter(repository));
        var antes = service.obtener("a");
        var despues = service.actualizar("a", "Mandarina", antes.version());
        assertThat(despues.id()).isEqualTo("a");
        assertThat(query.buscar("mandarina", PageRequest.of(0, 20)).getTotalElements()).isEqualTo(1);
        assertThat(query.buscar("tangerine", PageRequest.of(0, 20)).getTotalElements()).isZero();
        assertThatThrownBy(() -> service.crear("  MANDARINA ")).isInstanceOf(DuplicateKeyException.class);
        assertThat(service.guardarFotografia("a", new Imagen("nueva.png", "png", "BAUG")).fotografia().getFilepart())
                .isEqualTo("BAUG");
    }

    @Test
    void fotosGrandesNoSeAgrupanDentroDelDocumentoDelFacet() {
        template.updateMulti(new org.springframework.data.mongodb.core.query.Query(),
                new org.springframework.data.mongodb.core.query.Update().set("fotografia.filepart", "A".repeat(3_000_000)),
                CepaDocument.class);
        for (int i = 0; i < 5; i++) {
            String id = "grande" + i;
            template.insert(new CepaDocument(id, id, id, new ImagenDocument("foto.webp", "webp",
                    "A".repeat(3_000_000)), null, null, null));
            presentacion(id, "Bolsa", 5, 10000, 1, "ACTIVO");
        }
        assertThat(query.buscar("", PageRequest.of(0, 20)).getContent()).hasSize(7);
    }

    private void presentacion(String cepaId, String nombre, int contenido, int precio, int stock, String estado) {
        template.getCollection("productos_marketplace").insertOne(new Document("presentacionId", new ObjectId())
                .append("loteId", new ObjectId()).append("cepaId", cepaId).append("sku", UUID.randomUUID().toString())
                .append("presentacionNombre", nombre).append("contenidoUnidad", new Decimal128(BigDecimal.valueOf(contenido)))
                .append("unidadBase", "GRAMOS").append("precioVenta", new Decimal128(BigDecimal.valueOf(precio)))
                .append("stockDisponible", stock).append("estado", estado)
                .append("fotografia", new Document("nombre", "bolsa.png").append("extension", "png").append("filepart", "AQID")));
    }
}

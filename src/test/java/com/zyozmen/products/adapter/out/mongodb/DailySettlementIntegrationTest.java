package com.zyozmen.products.adapter.out.mongodb;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.zyozmen.products.adapter.in.web.AdminOrdersController;
import com.zyozmen.products.adapter.in.web.DailySettlementController;
import com.zyozmen.products.adapter.in.web.OrdersController;
import com.zyozmen.products.adapter.out.mongodb.document.ProviderAccumulatorsDocument;
import com.zyozmen.products.adapter.out.mongodb.document.DailySettlementSettingsDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProductMarketplaceDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProviderDocument;
import com.zyozmen.products.adapter.out.mongodb.document.UserDocument;
import com.zyozmen.products.adapter.out.mongodb.mapper.CategoryMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.mapper.OrderMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.mapper.ProviderMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.mapper.UserMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.LoteMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.repository.ProviderMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.repository.RevokedTokenMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.repository.TransaccionProveedorMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.sequence.SequenceGeneratorService;
import com.zyozmen.products.application.service.DailySettlementService;
import com.zyozmen.products.application.service.InventarioOrdenService;
import com.zyozmen.products.application.service.MarketplaceService;
import com.zyozmen.products.application.service.OrderService;
import com.zyozmen.products.application.service.OrderSettlementService;
import com.zyozmen.products.application.service.ProductService;
import com.zyozmen.products.config.SecurityConfig;
import com.zyozmen.products.domain.model.Container;
import com.zyozmen.products.domain.model.InventoryPresentation;
import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.model.LotePaymentState;
import com.zyozmen.products.exception.GlobalExceptionHandler;
import com.zyozmen.products.exception.OrderExceptionHandler;
import com.zyozmen.products.security.JwtAuthenticationFilter;
import com.zyozmen.products.security.JwtService;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "HU04_TEST_MONGO_URI", matches = ".+")
@SpringBootTest(classes = DailySettlementIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.main.banner-mode=off", "logging.level.org.mongodb.driver=WARN"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("hu04-t1-http-test")
class DailySettlementIntegrationTest {
    private static final String DATABASE = "hu04_t1_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final String PROVIDER_A = "6500000000000000000000ab";
    private static final String PROVIDER_B = "6500000000000000000000ac";
    private static final String URL = "/api/v1/settlements/daily-summary";
    private static final LocalDate DAY = LocalDate.of(2026, 10, 23);
    @Autowired private MongoTemplate template;
    @Autowired private TestRestTemplate http;
    @Autowired private JwtService jwt;
    @Autowired private DailySettlementIndexInitializer initializer;

    @BeforeEach
    void resetOwnDatabase() {
        template.remove(new Query(), ProviderAccumulatorsDocument.class);
        template.remove(new Query(), ProviderDocument.class);
        template.remove(new Query(), DailySettlementSettingsDocument.class);
        for (String collection : List.of("orders", "lotes", "productos_marketplace", "cepas", "provider_daily_movements", "Users")) {
            template.getCollection(collection).deleteMany(new Document());
        }
        initializer.run(null);
    }

    @AfterAll
    void cleanUp() {
        template.getDb().drop();
    }

    @Test
    void administradorConsultaResumenVacioRealConInicioPersistidoSinCrearFilas() {
        var response = get(URL, "ADMIN");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var body = response.getBody();
        assertThat(body.get("date").asText()).isEqualTo(LocalDate.now(ZoneId.of("America/Bogota")).toString());
        assertThat(body.get("timezone").asText()).isEqualTo("America/Bogota");
        assertThat(body.get("activationPending").asBoolean()).isFalse();
        assertThat(Instant.parse(body.get("activatedAt").asText())).isBeforeOrEqualTo(Instant.now());
        assertThat(body.get("content")).isEmpty();
        assertThat(body.get("page").asInt()).isZero();
        assertThat(body.get("size").asInt()).isEqualTo(20);
        assertThat(body.get("totalElements").asLong()).isZero();
        assertThat(body.get("totalPages").asInt()).isZero();
        assertThat(template.count(new Query(), ProviderAccumulatorsDocument.class)).isZero();
        assertThat(template.count(new Query(), ProviderDocument.class)).isZero();
    }

    @Test
    void seguridadRealJwtRechazaAnonimoClienteYTokenInvalido() {
        assertThat(http.getForEntity(URL, JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get(URL, "CLIENTE").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        var headers = new HttpHeaders();
        headers.setBearerAuth("token-invalido");
        assertThat(http.exchange(URL, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class)
                .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filtroPaginacionOrdenEstableYPrecisionUsanPersistenciaReal() {
        provider(PROVIDER_B, "B", "50000");
        provider(PROVIDER_A, "A", "15000.25");
        accumulator(PROVIDER_B, DAY, "2000", "0", "0", "0");
        accumulator(PROVIDER_A, DAY, "10000.25", "10000", "0", "1000");
        accumulator(PROVIDER_A, DAY.minusDays(1), "50", "0", "0", "0");

        var page = get(URL + "?date=" + DAY + "&size=1", "ADMIN").getBody();
        assertThat(page.get("totalElements").asLong()).isEqualTo(2);
        assertThat(page.get("totalPages").asInt()).isEqualTo(2);
        var a = page.get("content").get(0);
        assertThat(a.get("providerId").asText()).isEqualTo(PROVIDER_A);
        assertThat(a.get("providerName").asText()).isEqualTo("A");
        assertThat(a.get("totalSalesCount").asInt()).isEqualTo(2);
        assertThat(a.get("totalPqrCount").asInt()).isEqualTo(1);
        assertThat(a.get("grossAccumulatedAmount").isNumber()).isTrue();
        assertThat(a.get("grossAccumulatedAmount").decimalValue()).isEqualByComparingTo("20000.25");
        assertThat(a.get("pendingDailyAmount").decimalValue()).isEqualByComparingTo("19000.25");
        assertThat(a.get("suggestedPayableAmount").decimalValue()).isEqualByComparingTo("15000.25");

        var next = get(URL + "?date=" + DAY + "&size=1&page=1", "ADMIN").getBody();
        assertThat(next.get("content").get(0).get("providerId").asText()).isEqualTo(PROVIDER_B);
        var filtered = get(URL + "?date=" + DAY + "&providerId=" + PROVIDER_B.toUpperCase(), "ADMIN").getBody();
        assertThat(filtered.get("totalElements").asLong()).isEqualTo(1);
        assertThat(filtered.get("content").get(0).get("providerId").asText()).isEqualTo(PROVIDER_B);
        var beyond = get(URL + "?date=" + DAY + "&page=50", "ADMIN").getBody();
        assertThat(beyond.get("content")).isEmpty();
        assertThat(beyond.get("totalElements").asLong()).isEqualTo(2);
        assertThat(get(URL + "?date=" + DAY + "&providerId=6500000000000000000000ad", "ADMIN")
                .getBody().get("totalElements").asLong()).isZero();

        var stored = template.getCollection("provider_daily_accumulators")
                .find(new Document("providerId", new ObjectId(PROVIDER_A)).append("date", DAY.toString())).first();
        assertThat(stored.get("salesRecognizedAmount")).isEqualTo(new Decimal128(new BigDecimal("10000.25")));
        assertThat(stored.get("date")).isEqualTo(DAY.toString());
    }

    @Test
    void cambiosDeSaldoRealAfectanSugeridoSinReescribirAcumulado() {
        provider(PROVIDER_A, "Proveedor", "50000");
        accumulator(PROVIDER_A, DAY, "10000", "20000", "10000", "5000");
        var first = get(URL + "?date=" + DAY, "ADMIN").getBody().get("content").get(0);
        assertThat(first.get("accumulatedAmount").decimalValue()).isEqualByComparingTo("20000");
        assertThat(first.get("suggestedPayableAmount").decimalValue()).isEqualByComparingTo("15000");
        var before = template.getCollection("provider_daily_accumulators").find().first();

        template.getCollection("proveedores").updateOne(new Document("_id", new ObjectId(PROVIDER_A)),
                new Document("$set", new Document("financiero.saldoPendiente", new Decimal128(BigDecimal.ZERO))));
        var second = get(URL + "?date=" + DAY, "ADMIN").getBody().get("content").get(0);
        assertThat(second.get("grossAccumulatedAmount").decimalValue()).isEqualByComparingTo("30000");
        assertThat(second.get("suggestedPayableAmount").decimalValue()).isEqualByComparingTo("0");
        assertThat(template.getCollection("provider_daily_accumulators").find().first()).isEqualTo(before);
    }

    @Test
    void reversionTotalYPagosConservanJornadaVisibleYExceso() {
        provider(PROVIDER_A, "Proveedor", "100");
        accumulator(PROVIDER_A, DAY, "0", "10000", "10000", "8000");
        var body = get(URL + "?date=" + DAY, "ADMIN").getBody();
        assertThat(body.get("totalElements").asLong()).isEqualTo(1);
        var row = body.get("content").get(0);
        assertThat(row.get("accumulatedAmount").decimalValue()).isEqualByComparingTo("0");
        assertThat(row.get("pendingDailyAmount").decimalValue()).isEqualByComparingTo("0");
        assertThat(row.get("suggestedPayableAmount").decimalValue()).isEqualByComparingTo("0");
        assertThat(row.get("overpaidAmount").decimalValue()).isEqualByComparingTo("8000");
    }

    @Test
    void parametrosInvalidosDan400SinConsultasConvertidasEnExito() {
        for (String query : List.of("?page=-1", "?size=0", "?size=101", "?providerId=x",
                "?providerId=", "?date=2026-02-30", "?date=23-10-2026", "?page=abc")) {
            var response = get(URL + query, "ADMIN");
            assertThat(response.getStatusCode()).as(query).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().get("message").asText()).isNotBlank();
        }
    }

    @Test
    void indiceUnicoImpideDuplicadosEInicializarNoReiniciaActivacion() {
        accumulator(PROVIDER_A, DAY, "10", "0", "0", "0");
        assertThatThrownBy(() -> accumulator(PROVIDER_A, DAY, "20", "0", "0", "0"))
                .isInstanceOf(DuplicateKeyException.class);
        var activatedAt = Instant.parse("2026-10-23T05:00:00Z");
        template.save(new DailySettlementSettingsDocument(DailySettlementSettingsDocument.ID, activatedAt));
        initializer.run(null);
        initializer.run(null);
        var body = get(URL + "?date=2026-01-01", "ADMIN").getBody();
        assertThat(body.get("activationPending").asBoolean()).isFalse();
        assertThat(body.get("activatedAt").asText()).isEqualTo(activatedAt.toString());
        assertThat(template.count(new Query(), DailySettlementSettingsDocument.class)).isEqualTo(1);
        var indexes = template.indexOps(ProviderAccumulatorsDocument.class).getIndexInfo();
        assertThat(indexes).anySatisfy(index -> {
            assertThat(index.isUnique()).isTrue();
            assertThat(index.getIndexFields()).extracting(field -> field.getKey())
                    .containsExactly("providerId", "date");
        });
        assertThat(indexes).anySatisfy(index -> assertThat(index.getIndexFields())
                .extracting(field -> field.getKey()).containsExactly("date", "providerId"));
    }

    @Test
    void datosIncompletosNoSeOcultanComoProveedorConSaldoCero() {
        accumulator(PROVIDER_A, DAY, "10", "0", "0", "0");
        assertThat(get(URL + "?date=" + DAY, "ADMIN").getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        template.remove(new Query(), DailySettlementSettingsDocument.class);
        assertThat(get(URL, "ADMIN").getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void ventaCreadaYPagadaPorHttpApareceEnResumenRealYSuCancelacionEs409() {
        seedSale();
        var created = write("/api/orders", HttpMethod.POST, """
                {"items":[{"productId":"650000000000000000000401","quantity":3,
                 "unitPrice":15000,"lineTotal":45000}],"delivery":{},"subtotal":45000,
                 "taxAmount":7200,"total":52200}
                """, "CLIENTE");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var id = created.getBody().get("id").asText();
        assertThat(get(URL, "ADMIN").getBody().get("content")).isEmpty();
        var paid = write("/api/admin/orders/" + id + "/status", HttpMethod.PUT, "{\"status\":\"PAGADA\"}", "ADMIN");
        assertThat(paid.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(paid.getBody().toString()).doesNotContain("costoBasePorUnidad", "unitCost", "trazabilidad");
        var summary = get(URL, "ADMIN").getBody();
        var row = summary.get("content").get(0);
        assertThat(row.get("totalSalesCount").asInt()).isEqualTo(3);
        assertThat(row.get("grossAccumulatedAmount").decimalValue()).isEqualByComparingTo("30000");
        assertThat(row.get("suggestedPayableAmount").decimalValue()).isEqualByComparingTo("30000");
        assertThat(write("/api/admin/orders/" + id + "/status", HttpMethod.PUT,
                "{\"status\":\"PAGADA\"}", "ADMIN").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(write("/api/admin/orders/" + id + "/status", HttpMethod.PUT,
                "{\"status\":\"CANCELADA\"}", "ADMIN").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(write("/api/admin/orders/" + id + "/status", HttpMethod.PUT,
                "{\"status\":\"EMPACADA\"}", "ADMIN").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(write("/api/admin/orders/" + id + "/status", HttpMethod.PUT,
                "{\"status\":\"CANCELADA\"}", "ADMIN").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(get(URL, "ADMIN").getBody().get("content")).isEqualTo(summary.get("content"));
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isEqualTo(1);
        var movement = template.getCollection("provider_daily_movements").find().first();
        assertThat(movement.get("amount")).isEqualTo(new Decimal128(new BigDecimal("30000.00")));
        assertThat(movement.get("providerId")).isEqualTo(new ObjectId(PROVIDER_A));
        assertThat(template.getCollection("productos_marketplace").find().first().getInteger("stockDisponible")).isEqualTo(7);
        assertThat(template.findById(PROVIDER_A, ProviderDocument.class).getFinanciero().getSaldoPendiente())
                .isEqualByComparingTo("50000");
    }

    @Test
    void contraEntregaPorHttpSoloReconoceAlEntregarConCobroValido() {
        seedSale();
        var created = write("/api/orders", HttpMethod.POST, """
                {"items":[{"productId":"650000000000000000000401","quantity":2,
                 "unitPrice":15000,"lineTotal":30000}],"delivery":{},"subtotal":30000,
                 "taxAmount":4800,"total":34800}
                """, "CLIENTE");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var url = "/api/admin/orders/" + created.getBody().get("id").asText() + "/status";
        assertThat(write(url, HttpMethod.PUT, """
                {"status":"PAGADA","cashOnDelivery":true}
                """, "ADMIN").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get(URL, "ADMIN").getBody().get("content")).isEmpty();
        assertThat(write(url, HttpMethod.PUT, "{\"status\":\"EMPACADA\"}", "ADMIN").getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(write(url, HttpMethod.PUT, "{\"status\":\"ENTREGADA\"}", "ADMIN").getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(write(url, HttpMethod.PUT, """
                {"status":"ENTREGADA","cashAmount":1,"cardAmount":0}
                """, "ADMIN").getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(get(URL, "ADMIN").getBody().get("content")).isEmpty();
        var delivered = write(url, HttpMethod.PUT, """
                {"status":"ENTREGADA","cashAmount":30000,"cardAmount":4800}
                """, "ADMIN");
        assertThat(delivered.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(delivered.getBody().get("cashAmount").decimalValue()).isEqualByComparingTo("30000");
        assertThat(delivered.getBody().get("cardAmount").decimalValue()).isEqualByComparingTo("4800");
        var row = get(URL, "ADMIN").getBody().get("content").get(0);
        assertThat(row.get("totalSalesCount").asInt()).isEqualTo(2);
        assertThat(row.get("grossAccumulatedAmount").decimalValue()).isEqualByComparingTo("20000");
        assertThat(row.get("suggestedPayableAmount").decimalValue()).isEqualByComparingTo("20000");
        assertThat(write(url, HttpMethod.PUT, """
                {"status":"ENTREGADA","cashAmount":30000,"cardAmount":4800}
                """, "ADMIN").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isEqualTo(1);
        assertThat(template.getCollection("productos_marketplace").find().first().getInteger("stockDisponible"))
                .isEqualTo(8);
        assertThat(get(URL, "ADMIN").getBody().get("content").get(0)).isEqualTo(row);
    }

    @Test
    void pqrPorHttpReconoceAlCrearYCancelarConservaBrutoSinIngresoComercial() {
        seedSale();
        template.insert(UserDocument.builder()
                .username("hu04-http-order").role("CLIENTE").state("ACTIVO").build());
        var created = write("/api/admin/orders/pqr", HttpMethod.POST, """
                {"username":"hu04-http-order","items":[
                 {"productId":"650000000000000000000401","quantity":3}]}
                """, "ADMIN");
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var body = created.getBody();
        assertThat(body.get("status").asText()).isEqualTo("CREADA");
        assertThat(body.get("total").decimalValue()).isEqualByComparingTo("0");
        assertThat(body.get("subtotal").decimalValue()).isEqualByComparingTo("0");
        assertThat(body.get("items").get(0).get("unitPrice").decimalValue()).isEqualByComparingTo("0");
        assertThat(body.toString()).doesNotContain("costoBasePorUnidad", "unitCost", "trazabilidad");
        var row = get(URL, "ADMIN").getBody().get("content").get(0);
        assertThat(row.get("totalSalesCount").asInt()).isZero();
        assertThat(row.get("salesRecognizedAmount").decimalValue()).isEqualByComparingTo("0");
        assertThat(row.get("totalPqrCount").asInt()).isEqualTo(3);
        assertThat(row.get("pqrRecognizedAmount").decimalValue()).isEqualByComparingTo("30000");
        assertThat(row.get("suggestedPayableAmount").decimalValue()).isEqualByComparingTo("30000");
        var url = "/api/admin/orders/" + body.get("id").asText() + "/status";
        assertThat(write(url, HttpMethod.PUT, "{\"status\":\"EMPACADA\"}", "ADMIN").getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(get(URL, "ADMIN").getBody().get("content").get(0)).isEqualTo(row);
        var cancelled = write(url, HttpMethod.PUT,
                "{\"status\":\"CANCELADA\",\"reason\":\"PQR no requerida\"}", "ADMIN");
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        var after = get(URL, "ADMIN").getBody().get("content").get(0);
        assertThat(after.get("totalPqrCount").asInt()).isZero();
        assertThat(after.get("grossAccumulatedAmount").decimalValue()).isEqualByComparingTo("30000");
        assertThat(after.get("reversedAmount").decimalValue()).isEqualByComparingTo("30000");
        assertThat(after.get("accumulatedAmount").decimalValue()).isEqualByComparingTo("0");
        assertThat(after.get("suggestedPayableAmount").decimalValue()).isEqualByComparingTo("0");
        assertThat(write(url, HttpMethod.PUT, "{\"status\":\"CANCELADA\"}", "ADMIN").getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isEqualTo(2);
        assertThat(template.getCollection("productos_marketplace").find().first().getInteger("stockDisponible"))
                .isEqualTo(10);
        assertThat(template.findById(PROVIDER_A, ProviderDocument.class).getFinanciero().getSaldoPendiente())
                .isEqualByComparingTo("50000");
    }

    private void seedSale() {
        provider(PROVIDER_A, "Proveedor venta real", "50000");
        var repository = new MongoRepositoryFactory(template);
        var lots = new ProviderMongoAdapter(repository.getRepository(
                ProviderMongoRepository.class),
                repository.getRepository(LoteMongoRepository.class),
                repository.getRepository(TransaccionProveedorMongoRepository.class),
                new ProviderMongoMapper());
        var p = new InventoryPresentation("650000000000000000000401",
                "TAN10", "10g", BigDecimal.TEN, 10, 10, new BigDecimal("10000"),
                new BigDecimal("15000"), true, false, null);
        lots.save(Lote.builder().id("650000000000000000000201")
                .proveedorId(PROVIDER_A).costoTotalLote(new BigDecimal("100000")).montoPagadoLote(BigDecimal.ZERO)
                .saldoPendienteLote(new BigDecimal("50000")).estado("PROCESADO")
                .estadoPago(LotePaymentState.PAGO_PARCIAL)
                .contenedores(List.of(Container.builder()
                        .id("650000000000000000000301").cepaId("cepa-http").tipoCepa("Tangerine")
                        .unidadBase("GRAMOS").capacidadUnidadesBase(new BigDecimal("200"))
                        .cantidadDisponible(new BigDecimal("100")).costoBasePorUnidad(new BigDecimal("1000"))
                        .presentaciones(List.of(p)).build())).build());
        template.insert(new ProductMarketplaceDocument(
                null, p.presentacionId(), "650000000000000000000201", "Proveedor", "Tangerine", "Tangerine",
                p.sku(), p.precioVenta(), 10, null, "ACTIVO", "cepa-http", p.nombre(), p.contenidoUnidad(), "GRAMOS"));
        new SequenceGeneratorService(template)
                .nextSequence(SequenceGeneratorService.ORDER_SEQUENCE);
    }

    private ResponseEntity<JsonNode> write(String url, HttpMethod method, String body, String role) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(jwt.generateToken("hu04-http-order", role));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange(url, method, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private ResponseEntity<JsonNode> get(String url, String role) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(jwt.generateToken("hu04-test", role));
        return http.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
    }

    private void provider(String id, String name, String balance) {
        template.insert(ProviderDocument.builder().id(id).nombre(name).nit(id).financiero(
                ProviderDocument.FinancieroDocument.builder().totalDeuda(new BigDecimal(balance))
                        .totalPagado(BigDecimal.ZERO).saldoPendiente(new BigDecimal(balance)).build()).build());
    }

    private void accumulator(String provider, LocalDate date, String sales, String pqr, String reversed, String paid) {
        template.insert(new ProviderAccumulatorsDocument(null, provider, date.toString(), 2, 1,
                new BigDecimal(sales), new BigDecimal(pqr), new BigDecimal(reversed), new BigDecimal(paid)));
    }

    @Configuration(proxyBeanMethods = false)
    @Profile("hu04-t1-http-test")
    @EnableTransactionManagement
    @EnableMongoRepositories(basePackageClasses = RevokedTokenMongoRepository.class, mongoTemplateRef = "template")
    @EnableAutoConfiguration(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
            MongoRepositoriesAutoConfiguration.class})
    @Import({DailySettlementController.class, DailySettlementService.class, DailySettlementMongoAdapter.class,
            DailySettlementIndexInitializer.class, SecurityConfig.class, JwtAuthenticationFilter.class,
            JwtService.class, GlobalExceptionHandler.class, RevokedTokenMongoAdapter.class,
            OrderExceptionHandler.class,
            OrdersController.class,
            AdminOrdersController.class,
            OrderService.class,
            OrderSettlementService.class,
            InventarioOrdenService.class,
            MarketplaceService.class,
            ProductService.class,
            OrderMongoAdapter.class, ProviderMongoAdapter.class, UserMongoAdapter.class,
            MarketplaceMongoAdapter.class, MarketplaceCatalogoQuery.class, ProductMongoAdapter.class,
            CategoryMongoAdapter.class,
            SequenceGeneratorService.class,
            OrderMongoMapper.class,
            ProviderMongoMapper.class,
            UserMongoMapper.class,
            CategoryMongoMapper.class})
    static class TestApplication {
        @Bean(destroyMethod = "close")
        MongoClient client() {
            return MongoClients.create(System.getenv("HU04_TEST_MONGO_URI"));
        }

        @Bean
        MongoTemplate template(MongoClient client) throws Exception {
            var factory = new SimpleMongoClientDatabaseFactory(client, DATABASE);
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
            return new MongoTemplate(factory, converter);
        }

        @Bean
        MongoTransactionManager transactionManager(MongoTemplate template) {
            return new MongoTransactionManager(template.getMongoDatabaseFactory());
        }
    }
}

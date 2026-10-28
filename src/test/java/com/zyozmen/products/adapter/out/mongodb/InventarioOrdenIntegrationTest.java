package com.zyozmen.products.adapter.out.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.zyozmen.products.adapter.out.mongodb.document.*;
import com.zyozmen.products.adapter.out.mongodb.mapper.OrderMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.mapper.ProviderMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.*;
import com.zyozmen.products.adapter.out.mongodb.sequence.SequenceGeneratorService;
import com.zyozmen.products.application.service.InventarioOrdenService;
import com.zyozmen.products.application.service.OrderService;
import com.zyozmen.products.domain.exception.OrderConflictException;
import com.zyozmen.products.domain.exception.OrderProductException;
import com.zyozmen.products.domain.model.*;
import com.zyozmen.products.domain.port.in.MarketplaceUseCase;
import com.zyozmen.products.domain.port.in.OrderUseCase;
import com.zyozmen.products.domain.port.out.UserRepositoryPort;
import com.zyozmen.products.domain.port.out.InventarioOrdenPort;
import com.zyozmen.products.domain.port.out.LoteRepositoryPort;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.*;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "HU05_TEST_MONGO_URI", matches = ".+")
class InventarioOrdenIntegrationTest {
    private static final String LOTE = "650000000000000000000201";
    private static final String CONTENEDOR = "650000000000000000000301";
    private static final String PRESENTACION = "650000000000000000000401";
    private static final String PROVEEDOR = "650000000000000000000101";
    private MongoClient client;
    private MongoTemplate template;
    private MongoTransactionManager manager;
    private OrderMongoAdapter orders;
    private ProviderMongoAdapter lotes;
    private MarketplaceMongoAdapter marketplace;
    private MarketplaceUseCase catalogo;
    private UserRepositoryPort usuarios;
    private OrderUseCase service;
    private com.zyozmen.products.domain.port.out.DailySettlementRepositoryPort daily;
    private com.zyozmen.products.domain.port.out.OrderSettlementPort settlements;

    @BeforeEach
    void setUp() throws Exception {
        client = MongoClients.create(System.getenv("HU05_TEST_MONGO_URI"));
        var factory = new SimpleMongoClientDatabaseFactory(client, "hu05_t2_test_" + UUID.randomUUID().toString().replace("-", ""));
        var conversions = MongoCustomConversions.create(adapter -> {
            adapter.registerConverter(new Converter<BigDecimal, Decimal128>() {
                public Decimal128 convert(BigDecimal v) { return new Decimal128(v); }
            });
            adapter.registerConverter(new Converter<Decimal128, BigDecimal>() {
                public BigDecimal convert(Decimal128 v) { return v.bigDecimalValue(); }
            });
        });
        var context = new MongoMappingContext();
        context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        context.afterPropertiesSet();
        var converter = new MappingMongoConverter(new DefaultDbRefResolver(factory), context);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
        template = new MongoTemplate(factory, converter);
        manager = new MongoTransactionManager(factory);
        new DailySettlementIndexInitializer(template).run(null);
        var dailyProxy = new ProxyFactory(new DailySettlementMongoAdapter(template));
        dailyProxy.addAdvice(transactionInterceptor());
        daily = (com.zyozmen.products.domain.port.out.DailySettlementRepositoryPort) dailyProxy.getProxy();
        var settlementProxy = new ProxyFactory(new com.zyozmen.products.application.service.OrderSettlementService(daily));
        settlementProxy.addAdvice(transactionInterceptor());
        settlements = (com.zyozmen.products.domain.port.out.OrderSettlementPort) settlementProxy.getProxy();
        var repositories = new MongoRepositoryFactory(template);
        lotes = new ProviderMongoAdapter(repositories.getRepository(ProviderMongoRepository.class),
                repositories.getRepository(LoteMongoRepository.class),
                repositories.getRepository(TransaccionProveedorMongoRepository.class), new ProviderMongoMapper());
        marketplace = new MarketplaceMongoAdapter(repositories.getRepository(ProductMarketplaceMongoRepository.class),
                new MarketplaceCatalogoQuery(template), template);
        orders = new OrderMongoAdapter(repositories.getRepository(OrderMongoRepository.class), new OrderMongoMapper(),
                new SequenceGeneratorService(template), template);
        for (Class<?> type : List.of(OrderDocument.class, LoteDocument.class, ProductMarketplaceDocument.class)) {
            template.createCollection(type);
        }
        template.createCollection("sequences");
        new SequenceGeneratorService(template).nextSequence(SequenceGeneratorService.ORDER_SEQUENCE);
        catalogo = mock(MarketplaceUseCase.class);
        usuarios = mock(UserRepositoryPort.class);
        service = proxied(new OrderService(orders, catalogo, usuarios, inventario(lotes), settlements));
        lotes.save(Provider.builder().id(PROVEEDOR).nombre("Proveedor").nit(PROVEEDOR)
                .estado(ProviderState.ACTIVO).totalDeuda(new BigDecimal("100"))
                .totalPagado(BigDecimal.ZERO).saldoPendiente(new BigDecimal("100")).build());
        seed(LOTE, CONTENEDOR, PRESENTACION, 10);
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
    void pagoContraEntregaDescuentaYEntregaNoRepiteNiModificaDeuda() {
        var orden = orders.save(orden(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 3))));
        var pagada = service.changeStatus(orden.getId(), OrderStatus.PAGADA, "admin",
                new OrderUseCase.StatusChangeDetails(true, null, null, null));
        assertThat(pagada.isInventoryDeducted()).isTrue();
        stock(PRESENTACION, LOTE, 7);
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isZero();
        service.changeStatus(orden.getId(), OrderStatus.EMPACADA, "admin");
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isZero();
        var delivered = service.changeStatus(orden.getId(), OrderStatus.ENTREGADA, "admin",
                new OrderUseCase.StatusChangeDetails(null, BigDecimal.TEN, BigDecimal.ZERO, null));
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isEqualTo(1);
        var movement = template.getCollection("provider_daily_movements").find().first();
        assertThat(movement.getDate("timestamp").toInstant()).isEqualTo(
                delivered.getDeliveredAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        assertThat(movement.get("amount")).isEqualTo(new Decimal128(new BigDecimal("30.00")));
        stock(PRESENTACION, LOTE, 7);
        var lote = lotes.findLoteById(LOTE).orElseThrow();
        assertThat(lote.getSaldoPendienteLote()).isEqualByComparingTo("100");
        assertThat(lote.getContenedores().getFirst().getCantidadDisponible()).isEqualByComparingTo("50");
        assertThatThrownBy(() -> service.changeStatus(orden.getId(), OrderStatus.PAGADA, "admin"))
                .isInstanceOf(OrderConflictException.class);
        assertThatThrownBy(() -> service.changeStatus(orden.getId(), OrderStatus.ENTREGADA, "admin"))
                .isInstanceOf(OrderConflictException.class);
        stock(PRESENTACION, LOTE, 7);
    }

    @Test
    void cantidadesDuplicadasSeSumanYCancelarRestituyeSoloUnaVezInclusoSiSeDespublica() {
        var orden = pqr(2, 3);
        stock(PRESENTACION, LOTE, 5);
        template.getCollection("productos_marketplace").updateOne(new Document("presentacionId", new ObjectId(PRESENTACION)),
                new Document("$set", new Document("estado", "INACTIVO")));
        var cancelada = service.changeStatus(orden.getId(), OrderStatus.CANCELADA, "admin");
        assertThat(cancelada.isInventoryDeducted()).isFalse();
        stock(PRESENTACION, LOTE, 10);
        assertThatThrownBy(() -> service.changeStatus(orden.getId(), OrderStatus.CANCELADA, "admin"))
                .isInstanceOf(OrderConflictException.class);
        stock(PRESENTACION, LOTE, 10);
    }

    @Test
    void stockInsuficienteEnOtroLoteRevierteTodaLaOrden() {
        String l2 = "650000000000000000000202";
        String p2 = "650000000000000000000402";
        seed(l2, CONTENEDOR, p2, 1);
        var orden = orders.save(orden(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 2),
                item(l2, CONTENEDOR, p2, 2))));
        assertThatThrownBy(() -> service.changeStatus(orden.getId(), OrderStatus.PAGADA, "admin"))
                .isInstanceOf(OrderConflictException.class);
        stock(PRESENTACION, LOTE, 10);
        stock(p2, l2, 1);
        assertThat(orders.findById(orden.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CREADA);
        assertThat(orders.findById(orden.getId()).orElseThrow().isInventoryDeducted()).isFalse();
    }

    @Test
    void dosLotesYProveedoresSeActualizanEnUnaTransaccion() {
        String l2 = "650000000000000000000202";
        String p2 = "650000000000000000000402";
        seed(l2, CONTENEDOR, p2, 5);
        var otro = lotes.findLoteById(l2).orElseThrow();
        otro.setProveedorId("650000000000000000000102");
        lotes.save(otro);
        var item2 = item(l2, CONTENEDOR, p2, 2);
        var t = item2.getTrazabilidad();
        item2.setTrazabilidad(new PresentationTrace(otro.getProveedorId(), t.loteId(), t.contenedorId(),
                t.cepaId(), t.presentacionId(), t.contenidoUnidad(), t.unidadBase(),
                t.costoBasePorUnidad(), t.costoPresentacion()));
        var orden = orders.save(orden(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 2), item2)));
        service.changeStatus(orden.getId(), OrderStatus.PAGADA, "admin");
        stock(PRESENTACION, LOTE, 8);
        stock(p2, l2, 3);
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isEqualTo(2);
        assertThat(template.getCollection("provider_daily_accumulators").countDocuments()).isEqualTo(2);
    }

    @Test
    void proyeccionDesincronizadaRechazaSinCambiosParciales() {
        template.getCollection("productos_marketplace").updateOne(new Document("presentacionId", new ObjectId(PRESENTACION)),
                new Document("$set", new Document("stockDisponible", 4)));
        var orden = orders.save(orden(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 1))));
        assertThatThrownBy(() -> service.changeStatus(orden.getId(), OrderStatus.PAGADA, "admin"))
                .isInstanceOf(OrderConflictException.class);
        assertThat(lotes.findLoteById(LOTE).orElseThrow().getContenedores().getFirst()
                .getPresentaciones().getFirst().stockDisponible()).isEqualTo(10);
        assertThat(orders.findById(orden.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CREADA);
    }

    @Test
    void crearPqrDescuentaYElFalloAlGuardarOrdenRevierteAmbosStocks() {
        when(usuarios.existsByUsername("cliente")).thenReturn(true);
        when(catalogo.resolverPresentacion(PRESENTACION)).thenReturn(new MarketplaceUseCase.PresentacionVenta(
                "Tangerine", BigDecimal.TEN, item(LOTE, CONTENEDOR, PRESENTACION, 1).getTrazabilidad()));
        var pqr = service.createPqr("cliente", List.of(new OrderUseCase.PqrItemRequest(PRESENTACION, null, 2)), null, null);
        assertThat(pqr.isInventoryDeducted()).isTrue();
        assertThat(pqr.getTotal()).isEqualByComparingTo("0");
        stock(PRESENTACION, LOTE, 8);
        var failing = spy(orders);
        doThrow(new IllegalStateException("fallo de persistencia")).when(failing).save(any());
        var conFallo = proxied(new OrderService(failing, catalogo, usuarios, inventario(lotes), settlements));
        assertThatThrownBy(() -> conFallo.createPqr("cliente",
                List.of(new OrderUseCase.PqrItemRequest(PRESENTACION, null, 2)), null, null))
                .isInstanceOf(IllegalStateException.class).hasMessage("fallo de persistencia");
        stock(PRESENTACION, LOTE, 8);
        assertThat(template.count(new org.springframework.data.mongodb.core.query.Query(), OrderDocument.class)).isEqualTo(1);
    }

    @Test
    void ordenAnteriorSinTrazabilidadNoMueveStock() {
        var item = item(LOTE, CONTENEDOR, PRESENTACION, 1);
        item.setTrazabilidad(null);
        var orden = orders.save(orden(List.of(item)));
        assertThatThrownBy(() -> service.changeStatus(orden.getId(), OrderStatus.PAGADA, "admin"))
                .isInstanceOf(OrderProductException.class);
        stock(PRESENTACION, LOTE, 10);
    }

    @Test
    void movimientoDeInventarioExigeTransaccionDeLaOrden() {
        assertThatThrownBy(() -> inventario(lotes).descontar(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 1))))
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
        stock(PRESENTACION, LOTE, 10);
    }

    @Test
    void conflictoDeVersionDelLoteRevierteProyeccionYEstadoDeOrden() {
        var obsoletos = spy(lotes);
        doAnswer(invocacion -> {
            Lote lote = invocacion.getArgument(0);
            lote.setVersion(lote.getVersion() + 1);
            return invocacion.callRealMethod();
        }).when(obsoletos).save(any(Lote.class));
        var conConflicto = proxied(new OrderService(orders, catalogo, usuarios, inventario(obsoletos), settlements));
        var orden = orders.save(orden(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 1))));
        assertThatThrownBy(() -> conConflicto.changeStatus(orden.getId(), OrderStatus.PAGADA, "admin"))
                .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
        stock(PRESENTACION, LOTE, 10);
        assertThat(orders.findById(orden.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CREADA);
    }

    @Test
    void cancelarOrdenAnteriorPagadaSinMarcaNoCreaExistencias() {
        var historica = orden(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 1)));
        historica.setStatus(OrderStatus.PAGADA);
        var guardada = orders.save(historica);
        assertThatThrownBy(() -> service.changeStatus(guardada.getId(), OrderStatus.CANCELADA, "admin"))
                .isInstanceOf(OrderConflictException.class);
        stock(PRESENTACION, LOTE, 10);
    }

    @Test
    void ventaNormalSumaItemsRepetidosUsaSnapshotNoCostoActualYConservaSaldos() {
        var order = orders.save(orden(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 2),
                item(LOTE, CONTENEDOR, PRESENTACION, 3))));
        var lot = lotes.findLoteById(LOTE).orElseThrow();
        lot.setSaldoPendienteLote(BigDecimal.ZERO);
        lot.setMontoPagadoLote(lot.getCostoTotalLote());
        lot.setContenedores(List.of(lot.getContenedores().getFirst().toBuilder()
                .costoBasePorUnidad(new BigDecimal("900")).build()));
        lotes.save(lot);
        var provider = lotes.findById(PROVEEDOR).orElseThrow();
        provider.setSaldoPendiente(BigDecimal.ZERO);
        provider.setTotalPagado(provider.getTotalDeuda());
        lotes.save(provider);
        var paid = service.changeStatus(order.getId(), OrderStatus.PAGADA, "admin");
        var result = new com.zyozmen.products.application.service.DailySettlementService(daily)
                .summary(paid.getPaidAt().atZone(com.zyozmen.products.application.service.DailySettlementService.BUSINESS_ZONE)
                        .toLocalDate(), PROVEEDOR, 0, 20).providers().getContent().getFirst();
        assertThat(result.totalSalesCount()).isEqualTo(5);
        assertThat(result.grossAccumulatedAmount()).isEqualByComparingTo("50");
        assertThat(result.suggestedPayableAmount()).isEqualByComparingTo("0");
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isEqualTo(2);
        var stored = template.getCollection("provider_daily_movements").find().first();
        assertThat(stored.get("unitCost")).isEqualTo(new Decimal128(new BigDecimal("10.00")));
        assertThat(stored.get("baseUnitCost")).isEqualTo(new Decimal128(BigDecimal.ONE));
        assertThat(lotes.findLoteById(LOTE).orElseThrow().getSaldoPendienteLote()).isEqualByComparingTo("0");
        assertThat(lotes.findById(PROVEEDOR).orElseThrow().getSaldoPendiente()).isEqualByComparingTo("0");
        stock(PRESENTACION, LOTE, 5);
        assertThatThrownBy(() -> service.changeStatus(order.getId(), OrderStatus.PAGADA, "admin"))
                .isInstanceOf(OrderConflictException.class);
        service.changeStatus(order.getId(), OrderStatus.EMPACADA, "admin");
        service.changeStatus(order.getId(), OrderStatus.ENTREGADA, "admin");
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isEqualTo(2);
    }

    @Test
    void ordenAnteriorAlInicioSePagaSinReconocimientoNiReprocesamiento() {
        var order = orden(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 1)));
        order.setCreatedAt(daily.activationAt().orElseThrow().minusSeconds(1));
        order = orders.save(order);
        service.changeStatus(order.getId(), OrderStatus.PAGADA, "admin");
        stock(PRESENTACION, LOTE, 9);
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isZero();
        assertThat(template.getCollection("provider_daily_accumulators").countDocuments()).isZero();
    }

    @Test
    void ventasSucesivasIncrementanElMismoAcumuladoSinReemplazarMovimientos() {
        var first = orders.save(orden(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 2))));
        var second = orders.save(orden(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 3))));
        service.changeStatus(first.getId(), OrderStatus.PAGADA, "admin");
        service.changeStatus(second.getId(), OrderStatus.PAGADA, "admin");
        assertThat(template.getCollection("provider_daily_accumulators").countDocuments()).isEqualTo(1);
        var accumulator = template.getCollection("provider_daily_accumulators").find().first();
        assertThat(accumulator.getLong("totalSalesCount")).isEqualTo(5L);
        assertThat(accumulator.get("salesRecognizedAmount")).isEqualTo(new Decimal128(new BigDecimal("50.00")));
        assertThat(accumulator.get("pqrRecognizedAmount")).isEqualTo(new Decimal128(BigDecimal.ZERO));
        assertThat(accumulator.get("linkedPaymentsAmount")).isEqualTo(new Decimal128(BigDecimal.ZERO));
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isEqualTo(2);
        stock(PRESENTACION, LOTE, 5);
    }

    @Test
    void reconocimientoFueraDeTransaccionSeRechaza() {
        var order = orden(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 1)));
        order.setId(1L);
        order.setStatus(OrderStatus.PAGADA);
        order.setPaidAt(java.time.Instant.now());
        assertThatThrownBy(() -> settlements.recognizeOrderMovement(order))
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isZero();
    }

    @Test
    void ventasConcurrentesDeLotesDistintosNoPierdenIncrementosDelMismoProveedor() throws Exception {
        String secondLot = "650000000000000000000202";
        String secondPresentation = "650000000000000000000402";
        seed(secondLot, CONTENEDOR, secondPresentation, 10);
        var first = orders.save(orden(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 2))));
        var second = orders.save(orden(List.of(item(secondLot, CONTENEDOR, secondPresentation, 2))));
        var barrier = new CyclicBarrier(2);
        var synchronizedService = proxied(new OrderService(orders, catalogo, usuarios, inventario(lotes), order -> {
            try {
                barrier.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Se interrumpio la prueba concurrente", exception);
            } catch (BrokenBarrierException | TimeoutException exception) {
                throw new IllegalStateException("No se sincronizaron las ventas de prueba", exception);
            }
            settlements.recognizeOrderMovement(order);
        }));
        service = synchronizedService;
        var start = new CountDownLatch(1);
        List<String> results;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> competir(start, first.getId(), OrderStatus.PAGADA));
            var two = executor.submit(() -> competir(start, second.getId(), OrderStatus.PAGADA));
            start.countDown();
            results = List.of(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS));
        }
        assertThat(results).containsExactlyInAnyOrder("OK", "CONFLICTO");
        var retry = proxied(new OrderService(orders, catalogo, usuarios, inventario(lotes), settlements));
        var failedId = results.getFirst().equals("CONFLICTO") ? first.getId() : second.getId();
        assertThat(orders.findById(failedId).orElseThrow().getStatus()).isEqualTo(OrderStatus.CREADA);
        retry.changeStatus(failedId, OrderStatus.PAGADA, "admin");
        assertThat(template.getCollection("provider_daily_accumulators").countDocuments()).isEqualTo(1);
        var accumulator = template.getCollection("provider_daily_accumulators").find().first();
        assertThat(accumulator.getLong("totalSalesCount")).isEqualTo(4L);
        assertThat(accumulator.get("salesRecognizedAmount")).isEqualTo(new Decimal128(new BigDecimal("40.00")));
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isEqualTo(2);
        stock(PRESENTACION, LOTE, 8);
        stock(secondPresentation, secondLot, 8);
    }

    @Test
    void falloAlGuardarOrdenRevierteReconocimientoAcumuladoEInventario() {
        var order = orders.save(orden(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 2))));
        var failing = spy(orders);
        doThrow(new IllegalStateException("fallo al guardar")).when(failing).save(any());
        var failedService = proxied(new OrderService(failing, catalogo, usuarios, inventario(lotes), settlements));
        assertThatThrownBy(() -> failedService.changeStatus(order.getId(), OrderStatus.PAGADA, "admin"))
                .isInstanceOf(IllegalStateException.class).hasMessage("fallo al guardar");
        stock(PRESENTACION, LOTE, 10);
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isZero();
        assertThat(template.getCollection("provider_daily_accumulators").countDocuments()).isZero();
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CREADA);
    }

    @Test
    void snapshotInvalidoRevierteDescuentoSinReconocerCostoCero() {
        var invalid = item(LOTE, CONTENEDOR, PRESENTACION, 2);
        var t = invalid.getTrazabilidad();
        invalid.setTrazabilidad(new PresentationTrace(t.proveedorId(), t.loteId(), t.contenedorId(),
                t.cepaId(), t.presentacionId(), t.contenidoUnidad(), t.unidadBase(),
                t.costoBasePorUnidad(), new BigDecimal("999")));
        var order = orders.save(orden(List.of(invalid)));
        assertThatThrownBy(() -> service.changeStatus(order.getId(), OrderStatus.PAGADA, "admin"))
                .isInstanceOf(OrderProductException.class);
        stock(PRESENTACION, LOTE, 10);
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isZero();
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CREADA);
    }

    @Test
    void reconocimientoDuplicadoEsConflictoYNoSumaOtraVezNiDejaMovimientoParcial() {
        var order = orders.save(orden(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 2))));
        var paid = service.changeStatus(order.getId(), OrderStatus.PAGADA, "admin");
        var transaction = new org.springframework.transaction.support.TransactionTemplate(manager);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> settlements.recognizeOrderMovement(paid)))
                .isInstanceOf(OrderConflictException.class);
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isEqualTo(1);
        assertThat(template.getCollection("provider_daily_accumulators").find().first().getLong("totalSalesCount"))
                .isEqualTo(2L);
        stock(PRESENTACION, LOTE, 8);
    }

    @Test
    void ventaConcurrenteNoSobrevende() throws Exception {
        concurrente(false);
    }

    @Test
    void dosConfirmacionesConcurrentesDeLaMismaOrdenNoDuplicanDescuento() throws Exception {
        concurrente(true);
    }

    @Test
    void cancelacionesConcurrentesRestituyenSoloUnaVez() throws Exception {
        var orden = pqr(3);
        forzarLecturasConcurrentes();
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> competir(inicio, orden.getId(), OrderStatus.CANCELADA));
            var two = executor.submit(() -> competir(inicio, orden.getId(), OrderStatus.CANCELADA));
            inicio.countDown();
            assertThat(List.of(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("OK", "CONFLICTO");
        }
        stock(PRESENTACION, LOTE, 10);
        assertThat(orders.findById(orden.getId()).orElseThrow().isInventoryDeducted()).isFalse();
        assertThat(template.getCollection("provider_daily_movements").countDocuments(
                new Document("movementType", "PQR_REVERSAL"))).isEqualTo(1);
        var accumulator = template.getCollection("provider_daily_accumulators").find().first();
        assertThat(accumulator.getLong("totalPqrCount")).isZero();
        assertThat(accumulator.get("reversedAmount")).isEqualTo(new Decimal128(new BigDecimal("30.00")));
    }

    @Test
    void pqrReconoceItemsRepetidosSinVentasNiDeudaYConSaldoCero() {
        var provider = lotes.findById(PROVEEDOR).orElseThrow();
        provider.setSaldoPendiente(BigDecimal.ZERO);
        provider.setTotalPagado(provider.getTotalDeuda());
        lotes.save(provider);
        var order = pqr(2, 3);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREADA);
        assertThat(order.getSubtotal()).isEqualByComparingTo("0");
        assertThat(order.getTaxAmount()).isEqualByComparingTo("0");
        assertThat(order.getTotal()).isEqualByComparingTo("0");
        assertThat(order.getItems()).allSatisfy(i -> {
            assertThat(i.getUnitPrice()).isEqualByComparingTo("0");
            assertThat(i.getLineTotal()).isEqualByComparingTo("0");
        });
        var row = new com.zyozmen.products.application.service.DailySettlementService(daily)
                .summary(order.getCreatedAt().atZone(
                        com.zyozmen.products.application.service.DailySettlementService.BUSINESS_ZONE).toLocalDate(),
                        PROVEEDOR, 0, 20).providers().getContent().getFirst();
        assertThat(row.totalPqrCount()).isEqualTo(5);
        assertThat(row.totalSalesCount()).isZero();
        assertThat(row.grossAccumulatedAmount()).isEqualByComparingTo("50");
        assertThat(row.suggestedPayableAmount()).isEqualByComparingTo("0");
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isEqualTo(2);
        assertThat(lotes.findLoteById(LOTE).orElseThrow().getSaldoPendienteLote()).isEqualByComparingTo("100");
        stock(PRESENTACION, LOTE, 5);
    }

    @Test
    void cancelarPqrEmpacadaRevierteJornadaOriginalConAuditoriaYPagosIntactos() {
        var order = pqr(2, 3);
        var originalDay = java.time.LocalDate.now(
                com.zyozmen.products.application.service.DailySettlementService.BUSINESS_ZONE).minusDays(1).toString();
        var earlier = java.time.Instant.now().minusSeconds(86400);
        template.getCollection("provider_daily_movements").updateMany(new Document(),
                new Document("$set", new Document("date", originalDay)
                        .append("timestamp", java.util.Date.from(earlier))));
        template.getCollection("provider_daily_accumulators").updateOne(new Document(),
                new Document("$set", new Document("date", originalDay)
                        .append("linkedPaymentsAmount", new Decimal128(new BigDecimal("25")))));
        service.changeStatus(order.getId(), OrderStatus.EMPACADA, "admin");
        var lot = lotes.findLoteById(LOTE).orElseThrow();
        lot.setContenedores(List.of(lot.getContenedores().getFirst().toBuilder()
                .costoBasePorUnidad(new BigDecimal("999")).build()));
        lotes.save(lot);
        var cancelled = service.changeStatus(order.getId(), OrderStatus.CANCELADA, "admin",
                new OrderUseCase.StatusChangeDetails(null, null, null, "Reposicion no requerida"));
        var collection = template.getCollection("provider_daily_movements");
        assertThat(collection.countDocuments()).isEqualTo(4);
        var originals = collection.find(new Document("movementType", "PQR")).into(new java.util.ArrayList<>());
        var reversals = collection.find(new Document("movementType", "PQR_REVERSAL")).into(new java.util.ArrayList<>());
        assertThat(reversals).allSatisfy(r -> {
            assertThat(r.getString("date")).isEqualTo(originalDay);
            assertThat(r.getDate("timestamp").toInstant()).isEqualTo(
                    cancelled.getCancelledAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
            assertThat(originals).anySatisfy(o -> {
                assertThat(r.getObjectId("originalMovementId")).isEqualTo(o.getObjectId("_id"));
                assertThat(r.get("amount")).isEqualTo(o.get("amount"));
                assertThat(r.get("unitCost")).isEqualTo(o.get("unitCost"));
            });
        });
        var row = new com.zyozmen.products.application.service.DailySettlementService(daily)
                .summary(java.time.LocalDate.parse(originalDay), PROVEEDOR, 0, 20)
                .providers().getContent().getFirst();
        assertThat(row.totalPqrCount()).isZero();
        assertThat(row.grossAccumulatedAmount()).isEqualByComparingTo("50");
        assertThat(row.reversedAmount()).isEqualByComparingTo("50");
        assertThat(row.pendingDailyAmount()).isEqualByComparingTo("0");
        assertThat(row.linkedPaymentsAmount()).isEqualByComparingTo("25");
        assertThat(row.overpaidAmount()).isEqualByComparingTo("25");
        assertThat(template.getCollection("provider_daily_accumulators").countDocuments()).isEqualTo(1);
        stock(PRESENTACION, LOTE, 10);
        assertThatThrownBy(() -> service.changeStatus(order.getId(), OrderStatus.CANCELADA, "admin"))
                .isInstanceOf(OrderConflictException.class);
        var transaction = new org.springframework.transaction.support.TransactionTemplate(manager);
        assertThatThrownBy(() -> transaction.executeWithoutResult(
                status -> daily.reversePqr(order.getId(), java.time.Instant.now())))
                .isInstanceOf(OrderConflictException.class);
        assertThat(collection.countDocuments()).isEqualTo(4);
    }

    @Test
    void falloDespuesDeReconocerCreacionPqrRevierteOrdenStocksYAcumulado() {
        var failingService = proxied(new OrderService(orders, catalogo, usuarios, inventario(lotes), order -> {
            settlements.recognizeOrderMovement(order);
            throw new IllegalStateException("fallo posterior al reconocimiento");
        }));
        service = failingService;
        assertThatThrownBy(() -> pqr(2)).isInstanceOf(IllegalStateException.class)
                .hasMessage("fallo posterior al reconocimiento");
        assertThat(template.getCollection("orders").countDocuments()).isZero();
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isZero();
        assertThat(template.getCollection("provider_daily_accumulators").countDocuments()).isZero();
        stock(PRESENTACION, LOTE, 10);
    }

    @Test
    void falloAlGuardarCancelacionPqrRevierteRestitucionYReversionFinanciera() {
        var order = pqr(2);
        var failing = spy(orders);
        doThrow(new IllegalStateException("fallo al cancelar")).when(failing).save(any());
        var failingService = proxied(new OrderService(failing, catalogo, usuarios, inventario(lotes), settlements));
        assertThatThrownBy(() -> failingService.changeStatus(order.getId(), OrderStatus.CANCELADA, "admin"))
                .isInstanceOf(IllegalStateException.class).hasMessage("fallo al cancelar");
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CREADA);
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isEqualTo(1);
        var accumulator = template.getCollection("provider_daily_accumulators").find().first();
        assertThat(accumulator.getLong("totalPqrCount")).isEqualTo(2);
        assertThat(accumulator.get("reversedAmount")).isEqualTo(new Decimal128(BigDecimal.ZERO));
        stock(PRESENTACION, LOTE, 8);
    }

    @Test
    void acumuladoOriginalInconsistenteNoPermiteCancelacionParcial() {
        var order = pqr(2);
        template.getCollection("provider_daily_accumulators").updateOne(new Document(),
                new Document("$set", new Document("pqrRecognizedAmount", new Decimal128(BigDecimal.ONE))));
        assertThatThrownBy(() -> service.changeStatus(order.getId(), OrderStatus.CANCELADA, "admin"))
                .isInstanceOf(OrderConflictException.class);
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CREADA);
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isEqualTo(1);
        stock(PRESENTACION, LOTE, 8);
    }

    @Test
    void pqrHistoricaSinReconocimientoRestituyeStockSinFabricarReversion() {
        var order = pqr(2);
        template.getCollection("provider_daily_movements").deleteMany(new Document());
        template.getCollection("provider_daily_accumulators").deleteMany(new Document());
        service.changeStatus(order.getId(), OrderStatus.CANCELADA, "admin");
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isZero();
        assertThat(template.getCollection("provider_daily_accumulators").countDocuments()).isZero();
        stock(PRESENTACION, LOTE, 10);
    }

    @Test
    void pqrEntregadaNoSePuedeCancelarNiReconocerDosVeces() {
        var order = pqr(2);
        service.changeStatus(order.getId(), OrderStatus.EMPACADA, "admin");
        service.changeStatus(order.getId(), OrderStatus.ENTREGADA, "admin");
        assertThatThrownBy(() -> service.changeStatus(order.getId(), OrderStatus.CANCELADA, "admin"))
                .isInstanceOf(OrderConflictException.class);
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isEqualTo(1);
        stock(PRESENTACION, LOTE, 8);
    }

    @Test
    void pqrDeDosProveedoresReconoceYRevierteCadaAcumuladoSinMezclarlos() {
        String secondLot = "650000000000000000000202";
        String secondPresentation = "650000000000000000000402";
        String secondProvider = "650000000000000000000102";
        seed(secondLot, CONTENEDOR, secondPresentation, 10);
        var lot = lotes.findLoteById(secondLot).orElseThrow();
        lot.setProveedorId(secondProvider);
        lotes.save(lot);
        var snapshot = item(secondLot, CONTENEDOR, secondPresentation, 1).getTrazabilidad();
        when(usuarios.existsByUsername("cliente")).thenReturn(true);
        when(catalogo.resolverPresentacion(PRESENTACION)).thenReturn(new MarketplaceUseCase.PresentacionVenta(
                "Tangerine", BigDecimal.TEN, item(LOTE, CONTENEDOR, PRESENTACION, 1).getTrazabilidad()));
        when(catalogo.resolverPresentacion(secondPresentation)).thenReturn(new MarketplaceUseCase.PresentacionVenta(
                "Tangerine", BigDecimal.TEN, new PresentationTrace(secondProvider,
                snapshot.loteId(), snapshot.contenedorId(), snapshot.cepaId(), snapshot.presentacionId(),
                snapshot.contenidoUnidad(), snapshot.unidadBase(), snapshot.costoBasePorUnidad(),
                snapshot.costoPresentacion())));
        var order = service.createPqr("cliente", List.of(
                new OrderUseCase.PqrItemRequest(PRESENTACION, null, 2),
                new OrderUseCase.PqrItemRequest(secondPresentation, null, 3)), null, null);
        var accumulators = template.getCollection("provider_daily_accumulators");
        assertThat(accumulators.countDocuments()).isEqualTo(2);
        assertThat(accumulators.find(new Document("providerId", new ObjectId(PROVEEDOR))).first()
                .getLong("totalPqrCount")).isEqualTo(2);
        assertThat(accumulators.find(new Document("providerId", new ObjectId(secondProvider))).first()
                .getLong("totalPqrCount")).isEqualTo(3);
        service.changeStatus(order.getId(), OrderStatus.CANCELADA, "admin");
        assertThat(accumulators.find().into(new java.util.ArrayList<>())).allSatisfy(a -> {
            assertThat(a.getLong("totalPqrCount")).isZero();
            assertThat(a.get("reversedAmount")).isEqualTo(a.get("pqrRecognizedAmount"));
        });
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isEqualTo(4);
        stock(PRESENTACION, LOTE, 10);
        stock(secondPresentation, secondLot, 10);
    }

    @Test
    void acumuladoOriginalAusenteRevierteTodaLaCancelacion() {
        var order = pqr(2);
        template.getCollection("provider_daily_accumulators").deleteMany(new Document());
        assertThatThrownBy(() -> service.changeStatus(order.getId(), OrderStatus.CANCELADA, "admin"))
                .isInstanceOf(OrderConflictException.class);
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CREADA);
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isEqualTo(1);
        assertThat(template.getCollection("provider_daily_accumulators").countDocuments()).isZero();
        stock(PRESENTACION, LOTE, 8);
    }

    private void concurrente(boolean mismaOrden) throws Exception {
        var first = orders.save(orden(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 6))));
        var second = mismaOrden ? first : orders.save(orden(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 6))));
        forzarLecturasConcurrentes();
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> competir(inicio, first.getId(), OrderStatus.PAGADA));
            var two = executor.submit(() -> competir(inicio, second.getId(), OrderStatus.PAGADA));
            inicio.countDown();
            var resultados = List.of(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS));
            assertThat(resultados).containsExactlyInAnyOrder("OK", "CONFLICTO");
        }
        stock(PRESENTACION, LOTE, 4);
        assertThat(template.getCollection("orders").countDocuments(new Document("status", "PAGADA"))).isEqualTo(1);
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isEqualTo(1);
        assertThat(template.getCollection("provider_daily_accumulators").find().first().getLong("totalSalesCount"))
                .isEqualTo(6L);
    }

    private void forzarLecturasConcurrentes() {
        var barrera = new CyclicBarrier(2);
        var lecturas = new AtomicInteger();
        var lotesConcurrentes = spy(lotes);
        doAnswer(invocacion -> {
            var resultado = invocacion.callRealMethod();
            if (lecturas.incrementAndGet() <= 2) {
                barrera.await(10, TimeUnit.SECONDS);
            }
            return resultado;
        }).when(lotesConcurrentes).findLoteById(LOTE);
        service = proxied(new OrderService(orders, catalogo, usuarios, inventario(lotesConcurrentes), settlements));
    }

    private String competir(CountDownLatch inicio, Long id, OrderStatus estado) throws InterruptedException {
        inicio.await();
        try {
            service.changeStatus(id, estado, "admin");
            return "OK";
        } catch (OrderConflictException | org.springframework.dao.TransientDataAccessException exception) {
            return "CONFLICTO";
        } catch (org.springframework.data.mongodb.UncategorizedMongoDbException exception) {
            var respuesta = new com.zyozmen.products.exception.OrderExceptionHandler().handleMongoConflict(exception);
            assertThat(respuesta.getStatusCode().value()).isEqualTo(409);
            return "CONFLICTO";
        }
    }

    private OrderUseCase proxied(OrderService target) {
        var proxy = new ProxyFactory(target);
        proxy.addAdvice(transactionInterceptor());
        return (OrderUseCase) proxy.getProxy();
    }

    private InventarioOrdenPort inventario(LoteRepositoryPort repository) {
        var proxy = new ProxyFactory(new InventarioOrdenService(repository, marketplace));
        proxy.addAdvice(transactionInterceptor());
        return (InventarioOrdenPort) proxy.getProxy();
    }

    private TransactionInterceptor transactionInterceptor() {
        var interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(manager);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        return interceptor;
    }

    private void stock(String presentacionId, String loteId, int esperado) {
        assertThat(marketplace.findByPresentacionId(presentacionId).orElseThrow().stockDisponible()).isEqualTo(esperado);
        assertThat(lotes.findLoteById(loteId).orElseThrow().getContenedores().getFirst()
                .getPresentaciones().getFirst().stockDisponible()).isEqualTo(esperado);
    }

    private Order orden(List<OrderItem> items) {
        return Order.builder().username("cliente").status(OrderStatus.CREADA).items(items).createdAt(java.time.Instant.now())
                .statusHistory(List.of()).delivery(OrderDelivery.builder().build())
                .total(BigDecimal.TEN).subtotal(BigDecimal.TEN).taxAmount(BigDecimal.ZERO).build();
    }

    private Order pqr(int... quantities) {
        when(usuarios.existsByUsername("cliente")).thenReturn(true);
        when(catalogo.resolverPresentacion(PRESENTACION)).thenReturn(new MarketplaceUseCase.PresentacionVenta(
                "Tangerine", BigDecimal.TEN, item(LOTE, CONTENEDOR, PRESENTACION, 1).getTrazabilidad()));
        return service.createPqr("cliente", java.util.Arrays.stream(quantities)
                .mapToObj(q -> new OrderUseCase.PqrItemRequest(PRESENTACION, null, q)).toList(), null, null);
    }

    @Test
    void falloAlGuardarEntregaRevierteImportesYReconocimientoPeroConservaStockYaDescontado() {
        var order = packedCod();
        var failing = spy(orders);
        doThrow(new IllegalStateException("fallo de entrega")).when(failing).save(any());
        var failedService = proxied(new OrderService(failing, catalogo, usuarios, inventario(lotes), settlements));
        assertThatThrownBy(() -> failedService.changeStatus(order.getId(), OrderStatus.ENTREGADA, "admin",
                new OrderUseCase.StatusChangeDetails(null, BigDecimal.TEN, BigDecimal.ZERO, null)))
                .isInstanceOf(IllegalStateException.class).hasMessage("fallo de entrega");
        var persisted = orders.findById(order.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(OrderStatus.EMPACADA);
        assertThat(persisted.getDeliveredAt()).isNull();
        assertThat(persisted.getCashAmount()).isNull();
        assertThat(persisted.getCardAmount()).isNull();
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isZero();
        assertThat(template.getCollection("provider_daily_accumulators").countDocuments()).isZero();
        stock(PRESENTACION, LOTE, 8);
    }

    @Test
    void entregaConCobroInvalidoNoReconoceNiCambiaEstado() {
        var order = packedCod();
        for (var details : List.of(OrderUseCase.StatusChangeDetails.NONE,
                new OrderUseCase.StatusChangeDetails(null, BigDecimal.ONE, BigDecimal.ZERO, null),
                new OrderUseCase.StatusChangeDetails(null, new BigDecimal("-1"), new BigDecimal("11"), null))) {
            assertThatThrownBy(() -> service.changeStatus(order.getId(), OrderStatus.ENTREGADA, "admin", details))
                    .isInstanceOfAny(com.zyozmen.products.domain.exception.OrderValidationException.class,
                            IllegalArgumentException.class);
        }
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.EMPACADA);
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isZero();
        stock(PRESENTACION, LOTE, 8);
    }

    @Test
    void dosEntregasConcurrentesNoDuplicanElAcumuladoNiElCobro() throws Exception {
        var order = packedCod();
        var barrier = new CyclicBarrier(2);
        var concurrentService = proxied(new OrderService(orders, catalogo, usuarios, inventario(lotes), delivered -> {
            try {
                barrier.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            } catch (BrokenBarrierException | TimeoutException exception) {
                throw new IllegalStateException(exception);
            }
            settlements.recognizeOrderMovement(delivered);
        }));
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var operation = (java.util.concurrent.Callable<String>) () -> {
                start.await();
                try {
                    concurrentService.changeStatus(order.getId(), OrderStatus.ENTREGADA, "admin",
                            new OrderUseCase.StatusChangeDetails(null, BigDecimal.TEN, BigDecimal.ZERO, null));
                    return "OK";
                } catch (OrderConflictException | org.springframework.dao.TransientDataAccessException exception) {
                    return "CONFLICTO";
                } catch (org.springframework.data.mongodb.UncategorizedMongoDbException exception) {
                    assertThat(new com.zyozmen.products.exception.OrderExceptionHandler()
                            .handleMongoConflict(exception).getStatusCode().value()).isEqualTo(409);
                    return "CONFLICTO";
                }
            };
            var one = executor.submit(operation);
            var two = executor.submit(operation);
            start.countDown();
            assertThat(List.of(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("OK", "CONFLICTO");
        }
        assertThat(template.getCollection("provider_daily_movements").countDocuments()).isEqualTo(1);
        assertThat(template.getCollection("provider_daily_accumulators").find().first().getLong("totalSalesCount"))
                .isEqualTo(2L);
        assertThat(orders.findById(order.getId()).orElseThrow().getCashAmount()).isEqualByComparingTo("10");
        stock(PRESENTACION, LOTE, 8);
    }

    private Order packedCod() {
        var order = orders.save(orden(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 2))));
        service.changeStatus(order.getId(), OrderStatus.PAGADA, "admin",
                new OrderUseCase.StatusChangeDetails(true, null, null, null));
        return service.changeStatus(order.getId(), OrderStatus.EMPACADA, "admin");
    }

    private OrderItem item(String lote, String contenedor, String presentacion, int cantidad) {
        return OrderItem.builder().productId(presentacion).quantity(cantidad).name("Tangerine")
                .unitPrice(BigDecimal.TEN).lineTotal(BigDecimal.TEN)
                .trazabilidad(new PresentationTrace(PROVEEDOR, lote, contenedor, "cepa", presentacion,
                        BigDecimal.TEN, "GRAMOS", BigDecimal.ONE, BigDecimal.TEN)).build();
    }

    private void seed(String loteId, String contenedorId, String presentacionId, int stock) {
        var p = new InventoryPresentation(presentacionId, presentacionId, "10g", BigDecimal.TEN, stock, stock,
                BigDecimal.TEN, BigDecimal.TEN, true, false, null);
        lotes.save(Lote.builder().id(loteId).proveedorId(PROVEEDOR).estado("PROCESADO")
                .estadoPago(LotePaymentState.PENDIENTE).costoTotalLote(new BigDecimal("100"))
                .saldoPendienteLote(new BigDecimal("100")).montoPagadoLote(BigDecimal.ZERO)
                .contenedores(List.of(Container.builder().id(contenedorId).cepaId("cepa").tipoCepa("Tangerine")
                        .unidadBase("GRAMOS").capacidadUnidadesBase(new BigDecimal("150"))
                        .cantidadDisponible(new BigDecimal("50")).costoBasePorUnidad(BigDecimal.ONE)
                        .presentaciones(List.of(p)).build())).build());
        marketplace.insert(new ProductMarketplace(presentacionId, loteId, "Proveedor", "Tangerine", "Tangerine",
                presentacionId, BigDecimal.TEN, stock, null, "ACTIVO", "cepa", "10g", BigDecimal.TEN, "GRAMOS"));
    }
}

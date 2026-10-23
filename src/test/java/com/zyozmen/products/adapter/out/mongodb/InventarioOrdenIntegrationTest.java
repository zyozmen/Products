package com.zyozmen.products.adapter.out.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.zyozmen.products.adapter.out.mongodb.document.*;
import com.zyozmen.products.adapter.out.mongodb.mapper.OrderMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.mapper.ProveedorMongoMapper;
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
    private ProveedorMongoAdapter lotes;
    private MarketplaceMongoAdapter marketplace;
    private MarketplaceUseCase catalogo;
    private UserRepositoryPort usuarios;
    private OrderUseCase service;

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
        var repositories = new MongoRepositoryFactory(template);
        lotes = new ProveedorMongoAdapter(repositories.getRepository(ProveedorMongoRepository.class),
                repositories.getRepository(LoteMongoRepository.class),
                repositories.getRepository(TransaccionProveedorMongoRepository.class), new ProveedorMongoMapper());
        marketplace = new MarketplaceMongoAdapter(repositories.getRepository(ProductoMarketplaceMongoRepository.class),
                new MarketplaceCatalogoQuery(template), template);
        orders = new OrderMongoAdapter(repositories.getRepository(OrderMongoRepository.class), new OrderMongoMapper(),
                new SequenceGeneratorService(template), template);
        for (Class<?> type : List.of(OrderMongoDocument.class, LoteDocument.class, ProductoMarketplaceDocument.class)) {
            template.createCollection(type);
        }
        template.createCollection("sequences");
        new SequenceGeneratorService(template).nextSequence(SequenceGeneratorService.ORDER_SEQUENCE);
        catalogo = mock(MarketplaceUseCase.class);
        usuarios = mock(UserRepositoryPort.class);
        service = proxied(new OrderService(orders, catalogo, usuarios, inventario(lotes)));
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
        service.changeStatus(orden.getId(), OrderStatus.EMPACADA, "admin");
        service.changeStatus(orden.getId(), OrderStatus.ENTREGADA, "admin",
                new OrderUseCase.StatusChangeDetails(null, BigDecimal.TEN, BigDecimal.ZERO, null));
        stock(PRESENTACION, LOTE, 7);
        var lote = lotes.findLoteById(LOTE).orElseThrow();
        assertThat(lote.getSaldoPendienteLote()).isEqualByComparingTo("100");
        assertThat(lote.getContenedores().getFirst().getCantidadDisponible()).isEqualByComparingTo("50");
        assertThatThrownBy(() -> service.changeStatus(orden.getId(), OrderStatus.PAGADA, "admin"))
                .isInstanceOf(OrderConflictException.class);
        stock(PRESENTACION, LOTE, 7);
    }

    @Test
    void cantidadesDuplicadasSeSumanYCancelarRestituyeSoloUnaVezInclusoSiSeDespublica() {
        var orden = orders.save(orden(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 2),
                item(LOTE, CONTENEDOR, PRESENTACION, 3))));
        service.changeStatus(orden.getId(), OrderStatus.PAGADA, "admin");
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
        item2.setTrazabilidad(new TrazabilidadPresentacion(otro.getProveedorId(), t.loteId(), t.contenedorId(),
                t.cepaId(), t.presentacionId(), t.contenidoUnidad(), t.unidadBase(),
                t.costoBasePorUnidad(), t.costoPresentacion()));
        var orden = orders.save(orden(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 2), item2)));
        service.changeStatus(orden.getId(), OrderStatus.PAGADA, "admin");
        stock(PRESENTACION, LOTE, 8);
        stock(p2, l2, 3);
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
        var conFallo = proxied(new OrderService(failing, catalogo, usuarios, inventario(lotes)));
        assertThatThrownBy(() -> conFallo.createPqr("cliente",
                List.of(new OrderUseCase.PqrItemRequest(PRESENTACION, null, 2)), null, null))
                .isInstanceOf(IllegalStateException.class).hasMessage("fallo de persistencia");
        stock(PRESENTACION, LOTE, 8);
        assertThat(template.count(new org.springframework.data.mongodb.core.query.Query(), OrderMongoDocument.class)).isEqualTo(1);
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
        var conConflicto = proxied(new OrderService(orders, catalogo, usuarios, inventario(obsoletos)));
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
        service.changeStatus(guardada.getId(), OrderStatus.CANCELADA, "admin");
        stock(PRESENTACION, LOTE, 10);
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
        var orden = orders.save(orden(List.of(item(LOTE, CONTENEDOR, PRESENTACION, 3))));
        service.changeStatus(orden.getId(), OrderStatus.PAGADA, "admin");
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
        service = proxied(new OrderService(orders, catalogo, usuarios, inventario(lotesConcurrentes)));
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
        return Order.builder().username("cliente").status(OrderStatus.CREADA).items(items)
                .statusHistory(List.of()).delivery(OrderDelivery.builder().build())
                .total(BigDecimal.TEN).subtotal(BigDecimal.TEN).taxAmount(BigDecimal.ZERO).build();
    }

    private OrderItem item(String lote, String contenedor, String presentacion, int cantidad) {
        return OrderItem.builder().productId(presentacion).quantity(cantidad).name("Tangerine")
                .unitPrice(BigDecimal.TEN).lineTotal(BigDecimal.TEN)
                .trazabilidad(new TrazabilidadPresentacion(PROVEEDOR, lote, contenedor, "cepa", presentacion,
                        BigDecimal.TEN, "GRAMOS", BigDecimal.ONE, BigDecimal.TEN)).build();
    }

    private void seed(String loteId, String contenedorId, String presentacionId, int stock) {
        var p = new PresentacionInventario(presentacionId, presentacionId, "10g", BigDecimal.TEN, stock, stock,
                BigDecimal.TEN, BigDecimal.TEN, true, false, null);
        lotes.save(Lote.builder().id(loteId).proveedorId(PROVEEDOR).estado("PROCESADO")
                .estadoPago(EstadoPagoLote.PENDIENTE).costoTotalLote(new BigDecimal("100"))
                .saldoPendienteLote(new BigDecimal("100")).montoPagadoLote(BigDecimal.ZERO)
                .contenedores(List.of(Contenedor.builder().id(contenedorId).cepaId("cepa").tipoCepa("Tangerine")
                        .unidadBase("GRAMOS").capacidadUnidadesBase(new BigDecimal("150"))
                        .cantidadDisponible(new BigDecimal("50")).costoBasePorUnidad(BigDecimal.ONE)
                        .presentaciones(List.of(p)).build())).build());
        marketplace.insert(new ProductoMarketplace(presentacionId, loteId, "Proveedor", "Tangerine", "Tangerine",
                presentacionId, BigDecimal.TEN, stock, null, "ACTIVO", "cepa", "10g", BigDecimal.TEN, "GRAMOS"));
    }
}

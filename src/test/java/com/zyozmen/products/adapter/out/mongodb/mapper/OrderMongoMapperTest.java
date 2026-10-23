package com.zyozmen.products.adapter.out.mongodb.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyozmen.products.adapter.in.web.dto.OrderResponseDTO;
import com.zyozmen.products.domain.model.*;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMongoMapperTest {
    private final OrderMongoMapper mapper = new OrderMongoMapper();

    @Test
    void snapshotSeConservaEnDocumentoMongoYNoSeExponeAlCliente() throws Exception {
        var item = item();
        var snapshot = new TrazabilidadPresentacion("proveedor", "lote", "contenedor", "cepa", "presentacion",
                BigDecimal.TEN, "GRAMOS", new BigDecimal("1000"), new BigDecimal("10000.00"));
        item.setTrazabilidad(snapshot);
        var order = order(item);
        order.setInventoryDeducted(true);

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
        var converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
        var bson = new Document();
        converter.write(mapper.toDocument(order), bson);

        var stored = bson.getList("items", Document.class).getFirst().get("trazabilidad", Document.class);
        assertThat(stored.get("costoPresentacion")).isEqualTo(new Decimal128(new BigDecimal("10000.00")));
        var result = mapper.toDomain(converter.read(
                com.zyozmen.products.adapter.out.mongodb.document.OrderMongoDocument.class, bson));
        assertThat(result.getItems().getFirst().getTrazabilidad()).isEqualTo(snapshot);
        assertThat(result.isInventoryDeducted()).isTrue();
        var json = new ObjectMapper().findAndRegisterModules().writeValueAsString(OrderResponseDTO.from(result));
        assertThat(json).doesNotContain("trazabilidad", "costo", "proveedorId", "inventoryDeducted", "inventory_deducted");
    }

    @Test
    void ordenAnteriorSinTrazabilidadSigueSiendoLegible() {
        var anterior = mapper.toDocument(order(item()));
        var result = mapper.toDomain(anterior);
        assertThat(result.getItems().getFirst().getTrazabilidad()).isNull();
        assertThat(result.isInventoryDeducted()).isFalse();
        assertThat(result.getItems().getFirst().getProductId()).isEqualTo("12");
        assertThat(OrderResponseDTO.from(result).items()).hasSize(1);
    }

    private OrderItem item() {
        return OrderItem.builder().productId("12").name("Tangerine").quantity(1)
                .unitPrice(BigDecimal.TEN).lineTotal(BigDecimal.TEN).build();
    }

    private Order order(OrderItem item) {
        return Order.builder().id(1L).username("cliente").status(OrderStatus.CREADA)
                .delivery(OrderDelivery.builder().build()).items(List.of(item)).statusHistory(List.of())
                .subtotal(BigDecimal.TEN).taxAmount(BigDecimal.ZERO).total(BigDecimal.TEN).build();
    }
}

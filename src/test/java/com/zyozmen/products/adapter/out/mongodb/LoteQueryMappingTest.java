package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.LoteDocument;
import com.zyozmen.products.adapter.out.mongodb.repository.LoteMongoRepository;
import org.bson.Document;
import org.bson.BsonRegularExpression;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.util.json.ParameterBindingDocumentCodec;

import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class LoteQueryMappingTest {
    @Test
    void conteosFiltranProveedorYClasificanPorSaldoNoPorEstadoDeInventario() throws Exception {
        ObjectId proveedor = new ObjectId();
        for (String metodo : new String[]{"countPagados", "countPorPagar"}) {
            var annotation = LoteMongoRepository.class
                    .getMethod(metodo, ObjectId.class).getAnnotation(Query.class);
            assertThat(annotation.count()).isTrue();
            var filtro = new ParameterBindingDocumentCodec().decode(annotation.value(), new Object[]{proveedor});
            var saldo = metodo.equals("countPagados") ? 0 : new Document("$gt", 0);
            assertThat(filtro).isEqualTo(new Document("proveedorId", proveedor)
                    .append("saldoPendienteLote", saldo));
        }
    }

    @Test
    void filtroMantieneProveedorFueraDelOrYBuscaEnLosTresCamposAnidados() throws Exception {
        ObjectId proveedor = new ObjectId();
        String texto = Pattern.quote("Sativa.*");
        var annotation = LoteMongoRepository.class
                .getMethod("buscarLotes", ObjectId.class, String.class, Pageable.class).getAnnotation(Query.class);
        var filter = new ParameterBindingDocumentCodec().decode(annotation.value(), new Object[]{proveedor, texto});

        assertThat(filter.get("proveedorId")).isEqualTo(proveedor);
        assertThat(filter.keySet()).containsExactlyInAnyOrder("proveedorId", "$or");
        var alternativas = filter.getList("$or", Document.class);
        assertThat(alternativas).hasSize(3);
        assertThat(alternativas).extracting(d -> d.keySet().iterator().next()).containsExactly(
                "contenedores.cepa.nombre", "contenedores.presentaciones.nombre", "contenedores.presentaciones.sku");
        for (Document alternativa : alternativas) {
            var condicion = alternativa.get(alternativa.keySet().iterator().next(), BsonRegularExpression.class);
            assertThat(condicion.getOptions()).isEqualTo("i");
            assertThat(condicion.getPattern()).isEqualTo(texto);
            assertThat(Pattern.compile(condicion.getPattern(), Pattern.CASE_INSENSITIVE)
                    .matcher("Distribuidora SATIVA.* Premium").find()).isTrue();
            assertThat(Pattern.compile(condicion.getPattern(), Pattern.CASE_INSENSITIVE)
                    .matcher("Sativa Premium").find()).isFalse();
        }
    }

    @Test
    void listadoSinBusquedaIncluyeSoloElFiltroDelProveedor() throws Exception {
        ObjectId proveedor = new ObjectId();
        var annotation = LoteMongoRepository.class
                .getMethod("findLotes", ObjectId.class, Pageable.class).getAnnotation(Query.class);
        assertThat(new ParameterBindingDocumentCodec().decode(annotation.value(), new Object[]{proveedor}))
                .isEqualTo(new Document("proveedorId", proveedor));
    }

    @Test
    void ordenPorCamposDeApiSeTraduceAlEsquemaMongoConDesempatePorObjectId() throws Exception {
        var context = new MongoMappingContext();
        var conversions = MongoCustomConversions.create(adapter -> {});
        context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        context.afterPropertiesSet();
        var converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();
        var mapper = new QueryMapper(converter);
        var entity = context.getRequiredPersistentEntity(LoteDocument.class);
        for (var campo : Map.of("fechaIngreso", "fechaRecepcion", "codigoLote", "codigoLote",
                "costoTotalLote", "costoTotalCalculado", "saldoPendienteLote", "saldoPendienteLote").entrySet()) {
            for (int direccion : new int[]{1, -1}) {
                assertThat(mapper.getMappedSort(new Document(campo.getKey(), direccion).append("id", direccion), entity))
                        .isEqualTo(new Document(campo.getValue(), direccion).append("_id", direccion));
            }
        }
    }
}

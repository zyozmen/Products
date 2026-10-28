package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.domain.model.UnitMeasurement;
import com.zyozmen.products.domain.model.MarketplaceCatalogFilter;
import com.zyozmen.products.adapter.out.mongodb.document.ProductDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProductMarketplaceDocument;
import com.zyozmen.products.adapter.out.mongodb.mapper.ImageMongoMapper;
import com.zyozmen.products.domain.port.in.MarketplaceUseCase.ProductoCatalogo;
import com.zyozmen.products.domain.port.in.MarketplaceUseCase.PresentacionDetalle;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MarketplaceCatalogoQuery {
    private static final Pattern MEDIDA = Pattern.compile(
            "^(\\d{1,15}(?:[.,]\\d{1,3})?)\\s*(g|gr|gramo|gramos|ml|mililitro|mililitros|unidad|unidades)$");
    private final MongoTemplate template;

    public Page<ProductoCatalogo> buscar(String buscar, Pageable pageable) {
        return buscar(new MarketplaceCatalogFilter(buscar, null, null, null), pageable);
    }

    public Page<ProductoCatalogo> buscar(MarketplaceCatalogFilter filtro, Pageable pageable) {
        var operaciones = pipeline(filtro, pageable).stream()
                .<AggregationOperation>map(stage -> context -> stage).toList();
        var result = template.aggregate(Aggregation.newAggregation(operaciones),
                "productos_marketplace", Document.class).getUniqueMappedResult();
        if (result == null) {
            throw new IllegalStateException("La consulta de catalogo no devolvio el resultado esperado");
        }
        var totals = result.getList("total", Document.class);
        long total = totals.isEmpty() ? 0 : ((Number) totals.getFirst().get("count")).longValue();
        var content = result.getList("content", Document.class).stream()
                .map(d -> template.getConverter().read(ProductoCatalogo.class, d)).toList();
        if (content.isEmpty()) {
            return new PageImpl<>(content, pageable, total);
        }
        var cepas = template.find(Query.query(Criteria.where("id")
                        .in(content.stream().map(ProductoCatalogo::cepaId).toList())), ProductDocument.class)
                .stream().collect(Collectors.toMap(ProductDocument::id, Function.identity()));
        var fotos = template.find(fotosQuery(content), ProductMarketplaceDocument.class)
                .stream().collect(Collectors.toMap(ProductMarketplaceDocument::presentacionId, Function.identity()));
        var conFotos = content.stream().map(p -> {
            var cepa = cepas.get(p.cepaId());
            if (cepa == null) {
                throw new IllegalStateException("La cepa del catalogo ya no existe");
            }
            return new ProductoCatalogo(p.cepaId(), p.nombre(), ImageMongoMapper.toDomain(cepa.fotografia()),
                    p.precioDesde(), p.presentaciones().stream().map(opcion -> {
                        var foto = fotos.get(opcion.presentacionId());
                        if (foto == null) {
                            throw new IllegalStateException("La presentacion del catalogo ya no existe");
                        }
                        return new PresentacionDetalle(opcion.presentacionId(), opcion.sku(), opcion.nombre(),
                                opcion.contenidoUnidad(), opcion.unidadBase(), opcion.precioVenta(),
                                opcion.stockDisponible(), ImageMongoMapper.toDomain(foto.fotografia()));
                    }).toList());
        }).toList();
        return new PageImpl<>(conFotos, pageable, total);
    }

    private Query fotosQuery(List<ProductoCatalogo> content) {
        var query = Query.query(Criteria.where("presentacionId").in(content.stream()
                .flatMap(p -> p.presentaciones().stream()).map(PresentacionDetalle::presentacionId).toList()));
        query.fields().include("presentacionId").include("fotografia").include("stockDisponible");
        return query;
    }

    static List<Document> pipeline(String buscar, Pageable pageable) {
        return pipeline(new MarketplaceCatalogFilter(buscar, null, null, null), pageable);
    }

    static List<Document> pipeline(MarketplaceCatalogFilter filtro, Pageable pageable) {
        var stages = new ArrayList<Document>();
        stages.add(new Document("$match", new Document("estado", "ACTIVO")
                .append("stockDisponible", new Document("$gt", 0))));
        stages.add(new Document("$sort", new Document("contenidoUnidad", 1).append("sku", 1).append("_id", 1)));
        var presentacion = new Document("presentacionId", new Document("$toString", "$presentacionId"))
                .append("sku", "$sku").append("nombre", "$presentacionNombre")
                .append("contenidoUnidad", "$contenidoUnidad").append("unidadBase", "$unidadBase")
                .append("precioVenta", "$precioVenta").append("stockDisponible", "$stockDisponible");
        stages.add(new Document("$group", new Document("_id", "$cepaId")
                .append("precioDesde", new Document("$min", "$precioVenta"))
                .append("presentaciones", new Document("$push", presentacion))));
        stages.add(new Document("$lookup", new Document("from", "cepas")
                .append("localField", "_id").append("foreignField", "_id")
                .append("pipeline", List.of(new Document("$project",
                        new Document("nombre", 1).append("nombreNormalizado", 1).append("categoryIds", 1)))).append("as", "cepa")));
        stages.add(new Document("$unwind", "$cepa"));
        if (!filtro.categoryIds().isEmpty()) {
            stages.add(new Document("$match", new Document("cepa.categoryIds", new Document("$in", filtro.categoryIds()))));
        }
        if (filtro.minPrice() != null || filtro.maxPrice() != null) {
            var limites = new Document();
            if (filtro.minPrice() != null) limites.append("$gte", new Decimal128(filtro.minPrice()));
            if (filtro.maxPrice() != null) limites.append("$lte", new Decimal128(filtro.maxPrice()));
            stages.add(new Document("$match", new Document("presentaciones",
                    new Document("$elemMatch", new Document("precioVenta", limites)))));
        }
        String texto = filtro.buscar().toLowerCase(Locale.ROOT);
        if (!texto.isEmpty()) {
            var condiciones = new ArrayList<Document>();
            condiciones.add(contiene("$cepa.nombreNormalizado", texto));
            condiciones.add(new Document("$anyElementTrue", List.of(new Document("$map",
                    new Document("input", "$presentaciones").append("as", "p")
                            .append("in", contiene("$$p.nombre", texto))))));
            var medida = MEDIDA.matcher(texto);
            if (medida.matches()) {
                var coincide = new Document("$and", List.of(
                        new Document("$eq", List.of("$$p.contenidoUnidad",
                                new Decimal128(new BigDecimal(medida.group(1).replace(',', '.'))))),
                        new Document("$eq", List.of("$$p.unidadBase", UnitMeasurement.desde(medida.group(2)).name()))));
                condiciones.add(new Document("$anyElementTrue", List.of(new Document("$map",
                        new Document("input", "$presentaciones").append("as", "p").append("in", coincide)))));
            }
            stages.add(new Document("$match", new Document("$expr", new Document("$or", condiciones))));
        }
        stages.add(new Document("$sort", new Document("cepa.nombreNormalizado", 1).append("_id", 1)));
        var pagina = List.of(new Document("$skip", pageable.getOffset()),
                new Document("$limit", pageable.getPageSize()),
                new Document("$project", new Document("_id", 0).append("cepaId", "$_id")
                        .append("nombre", "$cepa.nombre")
                        .append("precioDesde", 1).append("presentaciones", 1)));
        // Las imagenes se cargan aparte para no superar 16 MiB en el documento del facet.
        stages.add(new Document("$facet", new Document("content", pagina)
                .append("total", List.of(new Document("$count", "count")))));
        return stages;
    }

    private static Document contiene(String campo, String texto) {
        return new Document("$gte", List.of(new Document("$indexOfCP", List.of(
                new Document("$toLower", new Document("$ifNull", List.of(campo, ""))),
                new Document("$literal", texto))), 0));
    }
}

package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.ImageDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProductDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProductMarketplaceDocument;
import com.zyozmen.products.domain.model.MarketplaceCatalogFilter;
import com.zyozmen.products.domain.port.in.MarketplaceUseCase.PresentacionDetalle;
import com.zyozmen.products.domain.port.in.MarketplaceUseCase.ProductoCatalogo;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.convert.MongoConverter;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarketplaceCatalogoQueryTest {

    @Test
    void mapsCatalogResultsWithStrainAndPresentationPhotos() {
        MongoTemplate template = mock(MongoTemplate.class);
        MongoConverter converter = mock(MongoConverter.class);
        MarketplaceCatalogoQuery query = new MarketplaceCatalogoQuery(template);
        var option = new PresentacionDetalle("presentation-1", "TAN-5", "Bolsa 5 g",
                BigDecimal.valueOf(5), "GRAMOS", BigDecimal.valueOf(8000), 3, null);
        var catalogEntry = new ProductoCatalogo("strain-1", "Tangerine", null,
                BigDecimal.valueOf(8000), List.of(option));
        when(template.getConverter()).thenReturn(converter);
        when(converter.read(eq(ProductoCatalogo.class), any(Document.class))).thenReturn(catalogEntry);
        when(template.aggregate(any(Aggregation.class), eq("productos_marketplace"), eq(Document.class)))
                .thenReturn(new AggregationResults<>(List.of(new Document("total",
                        List.of(new Document("count", 1))).append("content", List.of(new Document("cepaId", "strain-1")))),
                        new Document()));
        when(template.find(any(), eq(ProductDocument.class))).thenReturn(List.of(
                new ProductDocument("strain-1", "Tangerine", "tangerine",
                        new ImageDocument("strain.png", "png", "AQID"), null, null, 0L)));
        when(template.find(any(), eq(ProductMarketplaceDocument.class))).thenReturn(List.of(
                new ProductMarketplaceDocument("publication-1", "presentation-1", "lot-1",
                        "Provider", "Tangerine", "Tangerine", "TAN-5", BigDecimal.valueOf(8000), 3,
                        new ImageDocument("presentation.webp", "webp", "BAUG"), "ACTIVO",
                        "strain-1", "Bolsa 5 g", BigDecimal.valueOf(5), "GRAMOS")));

        var result = query.buscar(new MarketplaceCatalogFilter("tangerine", null, null, null),
                PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().fotografia().getNombre()).isEqualTo("strain.png");
        assertThat(result.getContent().getFirst().presentaciones().getFirst().fotografia().getNombre())
                .isEqualTo("presentation.webp");
    }

    @Test
    void returnsEmptyPageAndRejectsMissingAggregationResult() {
        MongoTemplate template = mock(MongoTemplate.class);
        MarketplaceCatalogoQuery query = new MarketplaceCatalogoQuery(template);
        when(template.aggregate(any(Aggregation.class), eq("productos_marketplace"), eq(Document.class)))
                .thenReturn(new AggregationResults<>(List.of(new Document("total", List.of())
                        .append("content", List.of())), new Document()));

        assertThat(query.buscar("tangerine", PageRequest.of(0, 5))).isEmpty();

        when(template.aggregate(any(Aggregation.class), eq("productos_marketplace"), eq(Document.class)))
                .thenReturn(new AggregationResults<>(List.of(), new Document()));
        assertThatThrownBy(() -> query.buscar("tangerine", PageRequest.of(0, 5)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void failsIfCatalogStrainOrPresentationWasRemovedDuringLookup() {
        MongoTemplate template = mock(MongoTemplate.class);
        MongoConverter converter = mock(MongoConverter.class);
        MarketplaceCatalogoQuery query = new MarketplaceCatalogoQuery(template);
        var option = new PresentacionDetalle("presentation-1", "TAN-5", "Bolsa 5 g",
                BigDecimal.valueOf(5), "GRAMOS", BigDecimal.valueOf(8000), 3, null);
        var catalogEntry = new ProductoCatalogo("strain-1", "Tangerine", null,
                BigDecimal.valueOf(8000), List.of(option));
        when(template.getConverter()).thenReturn(converter);
        when(converter.read(eq(ProductoCatalogo.class), any(Document.class))).thenReturn(catalogEntry);
        when(template.aggregate(any(Aggregation.class), eq("productos_marketplace"), eq(Document.class)))
                .thenReturn(new AggregationResults<>(List.of(new Document("total", List.of())
                        .append("content", List.of(new Document("cepaId", "strain-1")))), new Document()));
        when(template.find(any(), eq(ProductDocument.class))).thenReturn(List.of());

        assertThatThrownBy(() -> query.buscar("tangerine", PageRequest.of(0, 5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La cepa del catalogo");

        when(template.find(any(), eq(ProductDocument.class))).thenReturn(List.of(
                new ProductDocument("strain-1", "Tangerine", "tangerine", null, null, null, 0L)));
        when(template.find(any(), eq(ProductMarketplaceDocument.class))).thenReturn(List.of());
        assertThatThrownBy(() -> query.buscar("tangerine", PageRequest.of(0, 5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La presentacion del catalogo");
    }

    @Test
    void buildsUnfilteredPagedPipeline() {
        var stages = MarketplaceCatalogoQuery.pipeline((String) null, PageRequest.of(2, 10));

        assertThat(stages).hasSize(7);
        assertThat(stages.getFirst().get("$match", Document.class).getString("estado")).isEqualTo("ACTIVO");
        assertThat(stages.get(1)).containsKey("$sort");
        assertThat(stages.get(2)).containsKey("$group");
        assertThat(stages.get(3)).containsKey("$lookup");
        assertThat(stages.get(4)).containsKey("$unwind");
        var facet = stages.getLast().get("$facet", Document.class);
        assertThat(facet.getList("content", Document.class).getFirst().getLong("$skip")).isEqualTo(20);
        assertThat(facet.getList("content", Document.class).get(1).getInteger("$limit")).isEqualTo(10);
        assertThat(facet.getList("total", Document.class)).containsExactly(new Document("$count", "count"));
    }

    @Test
    void addsCategoryPriceAndLiteralTextFilters() {
        var stages = MarketplaceCatalogoQuery.pipeline(
                new com.zyozmen.products.domain.model.MarketplaceCatalogFilter(
                        "  TANGERINE  ", List.of(7L, 8L), new BigDecimal("5.00"), new BigDecimal("25.50")),
                PageRequest.of(0, 20));

        assertThat(stages).hasSize(10);
        assertThat(stages.get(5).get("$match", Document.class))
                .containsKey("cepa.categoryIds");
        var prices = stages.get(6).get("$match", Document.class)
                .get("presentaciones", Document.class)
                .get("$elemMatch", Document.class)
                .get("precioVenta", Document.class);
        assertThat(prices).containsKeys("$gte", "$lte");
        assertThat(stages.get(7).get("$match", Document.class).get("$expr")).isNotNull();
    }

    @Test
    void recognizesSupportedMeasurementSearchesAndUnboundedPriceLimits() {
        for (String search : List.of("10gr", "10,5 gramos", "2 ml", "1 unidad", "3 unidades")) {
            var stages = MarketplaceCatalogoQuery.pipeline(search, PageRequest.of(0, 20));
            assertThat(stages).hasSize(8);
            var expression = stages.get(5).get("$match", Document.class).get("$expr", Document.class);
            assertThat(expression.getList("$or", Object.class)).hasSize(3);
        }

        var minOnly = MarketplaceCatalogoQuery.pipeline(
                new com.zyozmen.products.domain.model.MarketplaceCatalogFilter(
                        "", null, BigDecimal.ONE, null), PageRequest.of(0, 20));
        var maxOnly = MarketplaceCatalogoQuery.pipeline(
                new com.zyozmen.products.domain.model.MarketplaceCatalogFilter(
                        "", null, null, BigDecimal.TEN), PageRequest.of(0, 20));
        assertThat(minOnly).hasSize(8);
        assertThat(maxOnly).hasSize(8);
    }

    @Test
    void treatsUnsupportedMeasurementAsPlainLiteralText() {
        var stages = MarketplaceCatalogoQuery.pipeline("10 cm", PageRequest.of(0, 20));
        var expression = stages.get(5).get("$match", Document.class).get("$expr", Document.class);

        assertThat(expression.getList("$or", Object.class)).hasSize(2);
    }
}

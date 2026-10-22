package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.ProductoMarketplaceDocument;
import com.zyozmen.products.adapter.out.mongodb.repository.ProductoMarketplaceMongoRepository;
import com.zyozmen.products.domain.model.ProductoMarketplace;
import com.zyozmen.products.domain.model.Imagen;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MarketplaceMongoAdapterTest {
    @Test
    void consultaPorIdDePresentacionNoPorIdDelCatalogoAnterior() {
        var repository = mock(ProductoMarketplaceMongoRepository.class);
        var adapter = new MarketplaceMongoAdapter(repository, mock(MarketplaceCatalogoQuery.class));
        String id = "650000000000000000000401";
        when(repository.findByPresentacionId(id)).thenReturn(Optional.of(
                new ProductoMarketplaceDocument("publicacion", id, "650000000000000000000201",
                        "Proveedor", "Tangerine", "Tangerine", "TAN10", BigDecimal.TEN, 5, null, "ACTIVO")));

        var result = adapter.findByPresentacionId(id).orElseThrow();
        assertThat(result.presentacionId()).isEqualTo(id);
        assertThat(result.loteId()).isEqualTo("650000000000000000000201");
        assertThatThrownBy(() -> adapter.findByPresentacionId("12")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.findByPresentacionId(null)).isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).findById(anyString());
    }

    @Test
    void consultaDisponiblesPorIdentidadPersistidaDeCepa() {
        var repository = mock(ProductoMarketplaceMongoRepository.class);
        var adapter = new MarketplaceMongoAdapter(repository, mock(MarketplaceCatalogoQuery.class));
        when(repository.findByCepaIdAndEstadoAndStockDisponibleGreaterThan("cepa", "ACTIVO", 0))
                .thenReturn(List.of());
        assertThat(adapter.findDisponiblesByCepaId("cepa")).isEmpty();
        verify(repository).findByCepaIdAndEstadoAndStockDisponibleGreaterThan("cepa", "ACTIVO", 0);
    }

    @Test
    void insertaProyeccionSinSobrescribirStockDeOtrasPresentaciones() {
        var repository = mock(ProductoMarketplaceMongoRepository.class);
        var adapter = new MarketplaceMongoAdapter(repository, mock(MarketplaceCatalogoQuery.class));
        var p = new ProductoMarketplace("650000000000000000000401", "650000000000000000000201",
                "Proveedor", "Sativa - Bolsa", "Sativa", "SAT-5", BigDecimal.TEN, 100,
                new Imagen("foto.png", "png", "AQID"), "ACTIVO");

        adapter.insert(p);

        var captor = ArgumentCaptor.forClass(ProductoMarketplaceDocument.class);
        verify(repository).insert(captor.capture());
        assertThat(captor.getValue().presentacionId()).isEqualTo(p.presentacionId());
        assertThat(captor.getValue().stockDisponible()).isEqualTo(100);
        assertThat(captor.getValue().sku()).isEqualTo("SAT-5");
        assertThat(captor.getValue().fotografia().getFilepart()).isEqualTo("AQID");
        assertThat(captor.getValue().fotografia().getNombre()).isEqualTo("foto.png");
        verify(repository, never()).save(any(ProductoMarketplaceDocument.class));
        when(repository.existsBySku("SAT-5")).thenReturn(true);
        when(repository.existsByPresentacionId(p.presentacionId())).thenReturn(true);
        assertThat(adapter.existsBySku("SAT-5")).isTrue();
        assertThat(adapter.existsByPresentacionId(p.presentacionId())).isTrue();
    }

    @Test
    void indicesUnicosBloqueanDuplicadosConcurrentes() {
        var template = mock(MongoTemplate.class);
        var indexes = mock(IndexOperations.class);
        when(template.indexOps(ProductoMarketplaceDocument.class)).thenReturn(indexes);
        var cepaIndexes = mock(IndexOperations.class);
        when(template.indexOps(com.zyozmen.products.adapter.out.mongodb.document.CepaDocument.class))
                .thenReturn(cepaIndexes);

        new MarketplaceIndexInitializer(template).run(null);

        var captor = ArgumentCaptor.forClass(IndexDefinition.class);
        verify(indexes, times(3)).ensureIndex(captor.capture());
        assertThat(captor.getAllValues().subList(0, 2)).allSatisfy(index ->
                assertThat(index.getIndexOptions().getBoolean("unique")).isTrue());
        assertThat(captor.getAllValues()).extracting(index -> index.getIndexKeys().keySet().iterator().next())
                .containsExactly("presentacionId", "sku", "estado");
        verify(cepaIndexes).ensureIndex(any());
    }
}

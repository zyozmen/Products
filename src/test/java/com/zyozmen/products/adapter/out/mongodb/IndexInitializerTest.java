package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.CatalogCategoryDocument;
import com.zyozmen.products.adapter.out.mongodb.document.DailySettlementSettingsDocument;
import com.zyozmen.products.adapter.out.mongodb.document.OrderDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProductDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProductMarketplaceDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProviderAccumulatorsDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProviderDailyMovementsDocument;
import com.zyozmen.products.adapter.out.mongodb.document.RevokedTokenDocument;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndexInitializerTest {

    @Test
    void createsOrderIndexesAndMigratesLegacyStatus() {
        MongoTemplate template = mock(MongoTemplate.class);
        IndexOperations indexes = mock(IndexOperations.class);
        when(template.indexOps(OrderDocument.class)).thenReturn(indexes);

        new OrderIndexInitializer(template).run(null);

        var definitions = org.mockito.ArgumentCaptor.forClass(IndexDefinition.class);
        verify(indexes, times(5)).ensureIndex(definitions.capture());
        assertThat(definitions.getAllValues()).hasSize(5);
        verify(template, times(3)).updateMulti(any(), any(), eq(OrderDocument.class));
    }

    @Test
    void createsDailySettlementIndexesAndInitializesActivation() {
        MongoTemplate template = mock(MongoTemplate.class);
        IndexOperations accumulators = mock(IndexOperations.class);
        IndexOperations movements = mock(IndexOperations.class);
        when(template.indexOps(ProviderAccumulatorsDocument.class)).thenReturn(accumulators);
        when(template.indexOps(ProviderDailyMovementsDocument.class)).thenReturn(movements);

        new DailySettlementIndexInitializer(template).run(null);

        verify(accumulators, times(2)).ensureIndex(any(IndexDefinition.class));
        verify(movements).ensureIndex(any(IndexDefinition.class));
        verify(template).upsert(any(), any(), eq(DailySettlementSettingsDocument.class));
        verify(template).updateFirst(any(), any(), eq(DailySettlementSettingsDocument.class));
    }

    @Test
    void createsCategoryMarketplaceAndRevocationIndexes() {
        MongoTemplate template = mock(MongoTemplate.class);
        IndexOperations categories = mock(IndexOperations.class);
        IndexOperations products = mock(IndexOperations.class);
        IndexOperations publications = mock(IndexOperations.class);
        IndexOperations revocations = mock(IndexOperations.class);
        when(template.indexOps(CatalogCategoryDocument.class)).thenReturn(categories);
        when(template.indexOps(ProductDocument.class)).thenReturn(products);
        when(template.indexOps(ProductMarketplaceDocument.class)).thenReturn(publications);
        when(template.indexOps(RevokedTokenDocument.class)).thenReturn(revocations);

        new CategoryIndexInitializer(template).run(null);
        new MarketplaceIndexInitializer(template).run(null);
        new RevokedTokenIndexInitializer(template).run(null);

        verify(categories).ensureIndex(any(IndexDefinition.class));
        verify(products, times(2)).ensureIndex(any(IndexDefinition.class));
        verify(publications, times(3)).ensureIndex(any(IndexDefinition.class));
        verify(revocations).ensureIndex(any(IndexDefinition.class));
    }
}

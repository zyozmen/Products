package com.zyozmen.products.domain.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class MarketplaceCatalogFilterTest {
    @Test
    void normalizaBusquedaYCategoriasSinConvertirLimitesDePrecio() {
        var filtro = new MarketplaceCatalogFilter("  10gr  ", List.of(1L, 1L, 2L), BigDecimal.ZERO, new BigDecimal("50000.00"));
        assertThat(filtro.buscar()).isEqualTo("10gr");
        assertThat(filtro.categoryIds()).containsExactly(1L, 2L);
        assertThat(filtro.maxPrice()).isEqualByComparingTo("50000");
        assertThat(new MarketplaceCatalogFilter(null, null, null, null).categoryIds()).isEmpty();
    }

    @Test
    void rechazaBusquedaCategoriasYPreciosFueraDelContrato() {
        assertThatThrownBy(() -> new MarketplaceCatalogFilter("x".repeat(101), null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        for (var ids : List.of(List.of(0L), List.of(-1L), Collections.<Long>singletonList(null), Collections.nCopies(101, 1L))) {
            assertThatThrownBy(() -> new MarketplaceCatalogFilter("", ids, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        for (String precio : List.of("-1", "0.001", "1000000000000000")) {
            assertThatThrownBy(() -> new MarketplaceCatalogFilter("", null, new BigDecimal(precio), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> new MarketplaceCatalogFilter("", null, BigDecimal.TEN, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

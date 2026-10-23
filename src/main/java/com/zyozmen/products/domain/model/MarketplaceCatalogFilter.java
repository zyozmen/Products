package com.zyozmen.products.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record MarketplaceCatalogFilter(String buscar, List<Long> categoryIds,
                                       BigDecimal minPrice, BigDecimal maxPrice) {
    public MarketplaceCatalogFilter {
        buscar = buscar == null ? "" : buscar.strip();
        if (buscar.length() > 100) {
            throw new IllegalArgumentException("La busqueda admite hasta 100 caracteres");
        }
        if (categoryIds != null && (categoryIds.size() > 100
                || categoryIds.stream().anyMatch(id -> id == null || id <= 0))) {
            throw new IllegalArgumentException("categoryIds admite hasta 100 IDs positivos");
        }
        categoryIds = categoryIds == null ? List.of() : categoryIds.stream().distinct().toList();
        validarPrecio(minPrice);
        validarPrecio(maxPrice);
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice no puede superar maxPrice");
        }
    }

    private static void validarPrecio(BigDecimal precio) {
        if (precio != null && (precio.signum() < 0 || precio.stripTrailingZeros().scale() > 2
                || precio.compareTo(BigDecimal.TEN.pow(15)) >= 0)) {
            throw new IllegalArgumentException("Los precios deben ser no negativos y admitir hasta 15 enteros y 2 decimales");
        }
    }
}

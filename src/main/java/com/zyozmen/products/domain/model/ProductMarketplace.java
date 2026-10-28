package com.zyozmen.products.domain.model;

import java.math.BigDecimal;

public record ProductMarketplace(String presentacionId, String loteId, String proveedorNombre,
                                  String nombreProducto, String cepaNombre, String sku,
                                  BigDecimal precioVenta, int stockDisponible, Image fotografia,
                                  String estado, String cepaId, String presentacionNombre,
                                  BigDecimal contenidoUnidad, String unidadBase) {
    public ProductMarketplace(String presentacionId, String loteId, String proveedorNombre,
                               String nombreProducto, String cepaNombre, String sku, BigDecimal precioVenta,
                               int stockDisponible, Image fotografia, String estado) {
        this(presentacionId, loteId, proveedorNombre, nombreProducto, cepaNombre, sku, precioVenta,
                stockDisponible, fotografia, estado, null, null, null, null);
    }
}

package com.zyozmen.products.domain.model;

import java.math.BigDecimal;

public record ProductoMarketplace(String presentacionId, String loteId, String proveedorNombre,
                                  String nombreProducto, String cepaNombre, String sku,
                                  BigDecimal precioVenta, int stockDisponible, Imagen fotografia,
                                  String estado, String cepaId, String presentacionNombre,
                                  BigDecimal contenidoUnidad, String unidadBase) {
    public ProductoMarketplace(String presentacionId, String loteId, String proveedorNombre,
                               String nombreProducto, String cepaNombre, String sku, BigDecimal precioVenta,
                               int stockDisponible, Imagen fotografia, String estado) {
        this(presentacionId, loteId, proveedorNombre, nombreProducto, cepaNombre, sku, precioVenta,
                stockDisponible, fotografia, estado, null, null, null, null);
    }
}

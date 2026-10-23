package com.zyozmen.products.adapter.out.mongodb.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

@Document("productos_marketplace")
public record ProductoMarketplaceDocument(
        @Id String id,
        @Field(targetType = FieldType.OBJECT_ID) String presentacionId,
        @Field(targetType = FieldType.OBJECT_ID) String loteId,
        String proveedorNombre, String nombreProducto, String cepaNombre, String sku,
        BigDecimal precioVenta, int stockDisponible, ImagenDocument fotografia, String estado,
        String cepaId, String presentacionNombre, BigDecimal contenidoUnidad, String unidadBase) {
    public ProductoMarketplaceDocument(String id, String presentacionId, String loteId, String proveedorNombre,
                                      String nombreProducto, String cepaNombre, String sku, BigDecimal precioVenta,
                                      int stockDisponible, ImagenDocument fotografia, String estado) {
        this(id, presentacionId, loteId, proveedorNombre, nombreProducto, cepaNombre, sku, precioVenta,
                stockDisponible, fotografia, estado, null, null, null, null);
    }
}

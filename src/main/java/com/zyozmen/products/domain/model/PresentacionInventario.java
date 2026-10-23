package com.zyozmen.products.domain.model;

import java.math.BigDecimal;

public record PresentacionInventario(
        String presentacionId, String sku, String nombre, BigDecimal contenidoUnidad,
        int unidadesCreadas, int stockDisponible, BigDecimal costoPresentacionCalculado,
        BigDecimal precioVenta, boolean activoMarketplace, boolean confirmacionMargenNegativo,
        Imagen fotografia) {
}

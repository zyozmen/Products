package com.zyozmen.products.domain.model;

import java.math.BigDecimal;

public record TrazabilidadPresentacion(
        String proveedorId, String loteId, String contenedorId, String cepaId,
        String presentacionId, BigDecimal contenidoUnidad, String unidadBase,
        BigDecimal costoBasePorUnidad, BigDecimal costoPresentacion) {
}

package com.zyozmen.products.domain.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder(toBuilder = true)
public class Contenedor {
    String id;
    String tipoCepa;
    String cepaId;
    String unidadBase;
    String presentacionId;
    BigDecimal capacidadUnidadesBase;
    BigDecimal cantidadDisponible;
    BigDecimal costoBasePorUnidad;
    BigDecimal subtotal;
    String estado;
    String codigoContenedor;
    BigDecimal precioBaseUnidad;
    @Builder.Default
    java.util.List<PresentacionInventario> presentaciones = java.util.List.of();
}

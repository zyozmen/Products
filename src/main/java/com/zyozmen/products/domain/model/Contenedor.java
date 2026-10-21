package com.zyozmen.products.domain.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class Contenedor {
    String id;
    String tipoCepa;
    String unidadBase;
    String presentacionId;
    BigDecimal capacidadUnidadesBase;
    BigDecimal cantidadDisponible;
    BigDecimal costoBasePorUnidad;
    BigDecimal subtotal;
    String estado;
}

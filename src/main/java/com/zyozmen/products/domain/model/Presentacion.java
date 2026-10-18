package com.zyozmen.products.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Presentacion {
    private String presentacionId;
    private int cantidadUnidades;
    private BigDecimal costoUnitario;
    private BigDecimal subtotal;
}

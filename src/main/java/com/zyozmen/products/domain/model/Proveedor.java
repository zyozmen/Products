package com.zyozmen.products.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proveedor {
    private String id;
    private String nombre;
    private String nit;
    private String telefono;
    private String email;
    private BigDecimal totalDeuda;
    private BigDecimal totalPagado;
    private BigDecimal saldoPendiente;
    private EstadoProveedor estado;
    private Instant fechaCreacion;
    private Instant ultimaActualizacion;
    private Long version;
}

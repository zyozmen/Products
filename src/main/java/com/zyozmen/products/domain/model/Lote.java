package com.zyozmen.products.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lote {
    private String id;
    private String proveedorId;
    private String codigoLote;
    private BigDecimal costoTotalLote;
    private BigDecimal montoPagadoLote;
    private BigDecimal saldoPendienteLote;
    private LotePaymentState estadoPago;
    private String estado;
    private List<Container> contenedores;
    // Conserva el detalle de lotes historicos al registrar pagos; no se usa en nuevas recepciones.
    private List<Presentation> presentaciones;
    private Instant fechaIngreso;
    private BigDecimal abonoInicial;
    private BigDecimal costoTotalEmpaquetado;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}

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
public class ProviderTransaction {
    private String id;
    private String proveedorId;
    private String loteId;
    private ProviderTransactionType tipo;
    private BigDecimal monto;
    private Instant fecha;
    private String observaciones;
}

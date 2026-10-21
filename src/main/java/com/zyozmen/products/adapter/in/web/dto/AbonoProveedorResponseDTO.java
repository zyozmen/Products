package com.zyozmen.products.adapter.in.web.dto;

import com.zyozmen.products.domain.model.TransaccionProveedor;

import java.math.BigDecimal;
import java.time.Instant;

public record AbonoProveedorResponseDTO(String id, Instant fecha, BigDecimal monto, String proveedorId,
                                        String loteId, String tipo, String observaciones) {
    public static AbonoProveedorResponseDTO from(TransaccionProveedor movimiento) {
        return new AbonoProveedorResponseDTO(movimiento.getId(), movimiento.getFecha(), movimiento.getMonto(),
                movimiento.getProveedorId(), movimiento.getLoteId(), movimiento.getTipo().name(),
                movimiento.getObservaciones());
    }
}

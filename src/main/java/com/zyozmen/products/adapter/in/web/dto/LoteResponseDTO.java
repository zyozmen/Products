package com.zyozmen.products.adapter.in.web.dto;

import com.zyozmen.products.domain.model.Lote;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record LoteResponseDTO(
        String id,
        String proveedorId,
        String codigoLote,
        BigDecimal costoTotalLote,
        BigDecimal montoPagadoLote,
        BigDecimal saldoPendienteLote,
        String estadoPago,
        List<Presentacion> presentaciones,
        Instant fechaIngreso) {

    public record Presentacion(String presentacionId, int cantidadUnidades, BigDecimal costoUnitario,
                               BigDecimal subtotal) {
    }

    public static LoteResponseDTO from(Lote l) {
        return new LoteResponseDTO(l.getId(), l.getProveedorId(), l.getCodigoLote(), l.getCostoTotalLote(),
                l.getMontoPagadoLote(), l.getSaldoPendienteLote(), l.getEstadoPago().name(),
                l.getPresentaciones().stream()
                        .map(p -> new Presentacion(p.getPresentacionId(), p.getCantidadUnidades(),
                                p.getCostoUnitario(), p.getSubtotal()))
                        .toList(),
                l.getFechaIngreso());
    }
}

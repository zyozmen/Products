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
        String estado,
        List<Contenedor> contenedores,
        Instant fechaIngreso) {

    public record Contenedor(String id, String tipoCepa, String unidadBase, String presentacionId,
                             BigDecimal capacidadUnidadesBase, BigDecimal cantidadDisponible,
                             BigDecimal costoBasePorUnidad, BigDecimal subtotal, String estado) {
    }

    public static LoteResponseDTO from(Lote l) {
        return new LoteResponseDTO(l.getId(), l.getProveedorId(), l.getCodigoLote(), l.getCostoTotalLote(),
                l.getMontoPagadoLote(), l.getSaldoPendienteLote(), l.getEstadoPago().name(),
                l.getEstado(),
                l.getContenedores().stream()
                        .map(c -> new Contenedor(c.getId(), c.getTipoCepa(), c.getUnidadBase(), c.getPresentacionId(),
                                c.getCapacidadUnidadesBase(), c.getCantidadDisponible(),
                                c.getCostoBasePorUnidad(), c.getSubtotal(), c.getEstado()))
                        .toList(),
                l.getFechaIngreso());
    }
}

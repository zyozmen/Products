package com.zyozmen.products.adapter.in.web.dto;

import com.zyozmen.products.domain.model.Proveedor;

import java.math.BigDecimal;
import java.time.Instant;

public record ProveedorResponseDTO(
        String id,
        String nombre,
        String nit,
        Contacto contacto,
        BigDecimal totalDeuda,
        BigDecimal totalPagado,
        BigDecimal saldoPendiente,
        String estado,
        Instant fechaCreacion,
        Instant ultimaActualizacion) {

    public record Contacto(String telefono, String email) {
    }

    public static ProveedorResponseDTO from(Proveedor p) {
        return new ProveedorResponseDTO(p.getId(), p.getNombre(), p.getNit(),
                new Contacto(p.getTelefono(), p.getEmail()), p.getTotalDeuda(), p.getTotalPagado(),
                p.getSaldoPendiente(), p.getEstado().name(), p.getFechaCreacion(), p.getUltimaActualizacion());
    }
}

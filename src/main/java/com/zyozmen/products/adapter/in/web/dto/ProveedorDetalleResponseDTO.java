package com.zyozmen.products.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.zyozmen.products.domain.port.in.ProveedorUseCase.DetalleProveedor;

public record ProveedorDetalleResponseDTO(
        @JsonUnwrapped ProveedorResponseDTO proveedor,
        long lotesPagados,
        long lotesPorPagar) {

    public static ProveedorDetalleResponseDTO from(DetalleProveedor detalle) {
        return new ProveedorDetalleResponseDTO(ProveedorResponseDTO.from(detalle.proveedor()),
                detalle.lotesPagados(), detalle.lotesPorPagar());
    }
}

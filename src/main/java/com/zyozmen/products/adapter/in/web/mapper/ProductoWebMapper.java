package com.zyozmen.products.adapter.in.web.mapper;

import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.adapter.in.web.dto.ProductoRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.ProductoResponseDTO;
import org.springframework.stereotype.Component;

/**
 * Mapper del adaptador web. Convierte entre los DTOs HTTP y el modelo de dominio.
 * El controlador no conoce el modelo de dominio directamente en las firmas
 * públicas de la API; esta clase hace el puente.
 */
@Component
public class ProductoWebMapper {

    public Producto toDomain(ProductoRequestDTO dto) {
        return Producto.builder()
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .precio(dto.getPrecio())
                .build();
    }

    public ProductoResponseDTO toResponseDTO(Producto producto) {
        return ProductoResponseDTO.builder()
                .id(producto.getId())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .precio(producto.getPrecio())
                .build();
    }
}

package com.zyozmen.products.adapter.out.persistence.mapper;

import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.adapter.out.persistence.entity.ProductoJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper de persistencia. Convierte entre el modelo de dominio puro
 * y la entidad JPA de infraestructura.
 */
@Component
public class ProductoPersistenceMapper {

    public Producto toDomain(ProductoJpaEntity entity) {
        return Producto.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .descripcion(entity.getDescripcion())
                .precio(entity.getPrecio())
                .build();
    }

    public ProductoJpaEntity toEntity(Producto domain) {
        return ProductoJpaEntity.builder()
                .id(domain.getId())
                .nombre(domain.getNombre())
                .descripcion(domain.getDescripcion())
                .precio(domain.getPrecio())
                .build();
    }
}

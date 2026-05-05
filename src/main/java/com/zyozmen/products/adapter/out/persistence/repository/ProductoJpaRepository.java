package com.zyozmen.products.adapter.out.persistence.repository;

import com.zyozmen.products.adapter.out.persistence.entity.ProductoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA. Es un detalle de infraestructura:
 * el dominio no lo conoce en absoluto.
 */
public interface ProductoJpaRepository extends JpaRepository<ProductoJpaEntity, Long> {
}

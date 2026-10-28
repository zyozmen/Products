package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductRepositoryPort {
    Product save(Product cepa);
    Optional<Product> findById(String id);
    Page<Product> findAll(Pageable pageable);
}

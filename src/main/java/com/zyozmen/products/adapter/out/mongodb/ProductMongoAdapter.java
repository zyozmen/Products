package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.ProductDocument;
import com.zyozmen.products.adapter.out.mongodb.mapper.ImageMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.ProductMongoRepository;
import com.zyozmen.products.domain.model.Product;
import com.zyozmen.products.domain.port.out.ProductRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductMongoAdapter implements ProductRepositoryPort {
    private final ProductMongoRepository repository;

    @Override
    public Product save(Product c) {
        return toDomain(repository.save(new ProductDocument(c.id(), c.nombre(), Product.normalizar(c.nombre()),
                ImageMongoMapper.toDocument(c.fotografia()), c.createdAt(), c.updatedAt(), c.version(), c.categoryIds())));
    }

    @Override
    public Optional<Product> findById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El ID de cepa es obligatorio");
        }
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Page<Product> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDomain);
    }

    private Product toDomain(ProductDocument c) {
        return new Product(c.id(), c.nombre(), ImageMongoMapper.toDomain(c.fotografia()),
                c.createdAt(), c.updatedAt(), c.version(), c.categoryIds());
    }
}

package com.zyozmen.products.adapter.out.mongodb;

import com.mongodb.client.MongoClient;
import com.zyozmen.products.adapter.out.mongodb.mapper.ProductoMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.ProductoMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.sequence.SequenceGeneratorService;
import com.zyozmen.products.config.MongoConfig;
import com.zyozmen.products.domain.model.Category;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.port.out.ProductoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de salida (Outbound Adapter) MongoDB.
 *
 * Implementa el puerto de salida definido en el dominio usando
 * Spring Data MongoDB. El dominio nunca ve esta clase: solo conoce la
 * interfaz ProductoRepositoryPort.
 *
 * Anotado con @Primary para que Spring lo prefiera sobre el adaptador JPA
 * cuando ambos estén en el classpath. Cambia el perfil activo para alternar
 * entre adaptadores sin modificar el dominio ni la capa de aplicación.
 *
 */
@Primary
@Service
@RequiredArgsConstructor
public class ProductoMongoAdapter implements ProductoRepositoryPort {

    private final ProductoMongoRepository mongoRepository;
    private final ProductoMongoMapper mapper;
    private final SequenceGeneratorService sequenceGenerator;

    @Autowired
    private final MongoConfig mongoConfig;

    @Override
    public List<Producto> findAll() {
        return mongoRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Producto> findById(Long id) {
        return mongoRepository.findById(id.toString()).map(mapper::toDomain);
    }

    @Override
    public Producto save(Producto producto) {
        if (producto.getId() == null) {
            long nextId = sequenceGenerator.nextSequence(SequenceGeneratorService.PRODUCTO_SEQUENCE);
            producto.setId(nextId);
            return mapper.toDomain(mongoRepository.save(mapper.toDocument(producto)));
        }

        // Update: preserve existing MongoDB _id to avoid duplicate key error
        return mongoRepository.findById(producto.getId())
                .map(existing -> mapper.toDomain(
                        mongoRepository.save(mapper.toDocument(producto, existing.getId()))))
                .orElseGet(() -> mapper.toDomain(mongoRepository.save(mapper.toDocument(producto))));
    }

    @Override
    public boolean existsById(Long id) {
        return mongoRepository.existsById(id.toString());
    }

    @Override
    public void deleteById(Long id) {
        mongoRepository.deleteById(id.toString());
    }

    @Override
    public List<Producto> findFeatured() {
        return mongoRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .filter(product -> product.getRanking().getAverageRating().compareTo(BigDecimal.valueOf(4L)) > 0)
                .limit(10)
                .collect(Collectors.toList());
    }

    @Override
    public List<Category> findAllCategories() {
        return mongoRepository.findDistinctCategories()
                .stream()
                .map(mapper::toCategoryDomain)
            .sorted(Comparator.comparing(Category::getCategoryId,
                Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .collect(Collectors.toList());
    }

    
}

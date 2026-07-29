package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.mapper.ProductoMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.document.ProductoMongoDocument;
import com.zyozmen.products.adapter.out.mongodb.repository.ProductoMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.sequence.SequenceGeneratorService;
import com.zyozmen.products.domain.model.Category;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.port.out.ProductoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

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
    private final MongoTemplate mongoTemplate;

    @Override
    public List<Producto> findAll() {
        return mongoRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<Producto> findAll(Pageable pageable) {
        return mongoRepository.findAll(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Producto> findAllByCategoryIds(List<Long> categoryIds, Pageable pageable) {
        return mongoRepository.findByCategoriesCategoryIdIn(categoryIds, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Producto> findAllFiltered(
            List<Long> categoryIds,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating,
            BigDecimal maxRating,
            String name,
            Pageable pageable) {

        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();


        addCategoryCriteria(categoryIds, criteria);

        addPriceCriteria(minPrice, maxPrice, criteria);

        if (minRating != null || maxRating != null) {
            addRatingCriteria(minRating, maxRating, criteria);
        }

        addNameCriteria(name, criteria);

        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, ProductoMongoDocument.class);
        query.with(pageable);
        List<Producto> content = mongoTemplate.find(query, ProductoMongoDocument.class)
                .stream()
                .map(mapper::toDomain)
                .toList();

        return new PageImpl<>(content, pageable, total);

    }

    private void addNameCriteria(String name, List<Criteria> criteria) {
        if (name != null && !name.isBlank()) {
            String normalized = Pattern.quote(name.trim());
            criteria.add(Criteria.where("name").regex(normalized, "i"));
        }
    }

    private void addRatingCriteria(BigDecimal minRating, BigDecimal maxRating, List<Criteria> criteria) {

            Criteria ratingCriteria = Criteria.where("ranking.average_rating");
            if (minRating != null)
                ratingCriteria.gte(minRating.doubleValue());
            if (maxRating != null)
                ratingCriteria.lte(maxRating.doubleValue());
            criteria.add(ratingCriteria);

    }

    private void addPriceCriteria(BigDecimal minPrice, BigDecimal maxPrice, List<Criteria> criteria) {
        if (minPrice != null || maxPrice != null) {
            Criteria priceCriteria = Criteria.where("price.current");
            if (minPrice != null)
                priceCriteria.gte(minPrice.doubleValue());
            if (maxPrice != null)
                priceCriteria.lte(maxPrice.doubleValue());
            criteria.add(priceCriteria);
        }
    }

    private void addCategoryCriteria(List<Long> categoryIds, List<Criteria> criteria) {
        if (categoryIds != null && !categoryIds.isEmpty()) {
            criteria.add(Criteria.where("categories.category_id").in(categoryIds));
        }
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
                .toList();
    }

    @Override
    public List<Category> findAllCategories() {
        return mongoRepository.findDistinctCategories()
                .stream()
                .map(mapper::toCategoryDomain)
                .sorted(Comparator.comparing(Category::getCategoryId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

}

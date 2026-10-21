package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.mapper.CategoryMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.CategoryMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.sequence.SequenceGeneratorService;
import com.zyozmen.products.domain.model.CatalogCategory;
import com.zyozmen.products.domain.port.out.CategoryRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CategoryMongoAdapter implements CategoryRepositoryPort {
    private final CategoryMongoRepository repository;
    private final CategoryMongoMapper mapper;
    private final SequenceGeneratorService sequenceGenerator;

    @Override
    public CatalogCategory save(CatalogCategory category) {
        if (category.getCategoryId() == null) {
            category.setCategoryId(sequenceGenerator.nextSequence(SequenceGeneratorService.CATEGORY_SEQUENCE));
        }
        return mapper.toDomain(repository.save(mapper.toDocument(category)));
    }

    @Override
    public Optional<CatalogCategory> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<CatalogCategory> findAll() {
        return repository.findAll(Sort.by("name")).stream().map(mapper::toDomain).toList();
    }
}

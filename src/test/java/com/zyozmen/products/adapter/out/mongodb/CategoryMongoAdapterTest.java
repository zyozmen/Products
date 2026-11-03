package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.CatalogCategoryDocument;
import com.zyozmen.products.adapter.out.mongodb.mapper.CategoryMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.CategoryMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.sequence.SequenceGeneratorService;
import com.zyozmen.products.domain.model.CatalogCategory;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryMongoAdapterTest {

    private final CategoryMongoRepository repository = mock(CategoryMongoRepository.class);
    private final SequenceGeneratorService sequence = mock(SequenceGeneratorService.class);
    private final CategoryMongoAdapter adapter =
            new CategoryMongoAdapter(repository, new CategoryMongoMapper(), sequence);

    @Test
    void assignsSequenceWhenCreatingCategoryAndPreservesExistingId() {
        var category = CatalogCategory.builder().name("Flores").build();
        var document = CatalogCategoryDocument.builder().categoryId(7L).name("Flores").build();
        when(sequence.nextSequence(SequenceGeneratorService.CATEGORY_SEQUENCE)).thenReturn(7L);
        when(repository.save(any(CatalogCategoryDocument.class))).thenReturn(document);

        assertThat(adapter.save(category).getCategoryId()).isEqualTo(7L);
        assertThat(category.getCategoryId()).isEqualTo(7L);

        var existing = CatalogCategory.builder().categoryId(9L).name("Extractos").build();
        var existingDocument = CatalogCategoryDocument.builder().categoryId(9L).name("Extractos").build();
        when(repository.save(any(CatalogCategoryDocument.class))).thenReturn(existingDocument);
        assertThat(adapter.save(existing).getCategoryId()).isEqualTo(9L);
        verify(sequence).nextSequence(SequenceGeneratorService.CATEGORY_SEQUENCE);
    }

    @Test
    void mapsCategoryLookupsAndSortedListing() {
        var document = CatalogCategoryDocument.builder().categoryId(7L).name("Flores").build();
        when(repository.findById(7L)).thenReturn(Optional.of(document));
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(document));

        assertThat(adapter.findById(7L).orElseThrow().getName()).isEqualTo("Flores");
        assertThat(adapter.findById(8L)).isEmpty();
        assertThat(adapter.findAll()).extracting(CatalogCategory::getName).containsExactly("Flores");
        verify(repository).findAll(any(Sort.class));
        verify(repository, never()).findAll();
    }
}

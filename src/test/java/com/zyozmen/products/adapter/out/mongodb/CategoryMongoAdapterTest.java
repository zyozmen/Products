package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.CatalogCategoryDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProductoMongoDocument;
import com.zyozmen.products.adapter.out.mongodb.mapper.CategoryMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.CategoryMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.sequence.SequenceGeneratorService;
import com.zyozmen.products.domain.model.CatalogCategory;
import com.zyozmen.products.domain.model.Imagen;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.bson.Document;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryMongoAdapterTest {
    @Mock
    private CategoryMongoRepository repository;
    @Mock
    private SequenceGeneratorService sequenceGenerator;
    @Spy
    private CategoryMongoMapper mapper = new CategoryMongoMapper();
    @InjectMocks
    private CategoryMongoAdapter adapter;

    @Test
    void bsonUsesNumericCategoryIdAndProductReferenceWithoutChangingCollection() throws Exception {
        var context = new MongoMappingContext();
        context.setSimpleTypeHolder(MongoCustomConversions.create(configuration -> {}).getSimpleTypeHolder());
        context.afterPropertiesSet();
        var converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
        converter.afterPropertiesSet();
        Document category = new Document();
        Document product = new Document();

        converter.write(CatalogCategoryDocument.builder().categoryId(7L).name("Audio").build(), category);
        converter.write(ProductoMongoDocument.builder().id("1").categoryId(7L).build(), product);

        assertThat(category).containsEntry("_id", 7L).containsEntry("name", "Audio");
        assertThat(product).containsEntry("categoryId", 7L);
        assertThat(context.getRequiredPersistentEntity(CatalogCategoryDocument.class).getCollection()).isEqualTo("categories");
        assertThat(context.getRequiredPersistentEntity(ProductoMongoDocument.class).getCollection()).isEqualTo("Products");
        assertThat(converter.read(CatalogCategoryDocument.class, category).getCategoryId()).isEqualTo(7L);
        assertThat(converter.read(ProductoMongoDocument.class, product).getCategoryId()).isEqualTo(7L);
    }

    @Test
    void createsCategoryUsingNumericSequence() {
        var category = CatalogCategory.builder().name("Audio").image(new Imagen("audio.png", "png", "AQID")).build();
        when(sequenceGenerator.nextSequence(SequenceGeneratorService.CATEGORY_SEQUENCE)).thenReturn(7L);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = adapter.save(category);

        assertThat(saved.getCategoryId()).isEqualTo(7L);
        assertThat(saved.getImage()).isEqualTo(category.getImage());
        var captor = ArgumentCaptor.forClass(CatalogCategoryDocument.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCategoryId()).isEqualTo(7L);
        assertThat(captor.getValue().getName()).isEqualTo("Audio");
    }

    @Test
    void updateKeepsExistingNumericId() {
        var category = CatalogCategory.builder().categoryId(7L).name("Audio").build();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(adapter.save(category).getCategoryId()).isEqualTo(7L);
        verifyNoInteractions(sequenceGenerator);
    }

    @Test
    void readsIndependentCategoriesSortedByNameAndFindsById() {
        var document = CatalogCategoryDocument.builder().categoryId(7L).name("Audio").build();
        when(repository.findAll(Sort.by("name"))).thenReturn(List.of(document));
        when(repository.findById(7L)).thenReturn(Optional.of(document));
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThat(adapter.findAll()).extracting(CatalogCategory::getName).containsExactly("Audio");
        assertThat(adapter.findById(7L)).isPresent();
        assertThat(adapter.findById(99L)).isEmpty();
    }

    @Test
    void duplicateNameFailureIsNotSwallowed() {
        when(repository.save(any())).thenThrow(new DuplicateKeyException("duplicate name"));

        assertThatThrownBy(() -> adapter.save(CatalogCategory.builder().categoryId(7L).name("Audio").build()))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void initializesUniqueNameAndProductReferenceIndexes() {
        MongoTemplate template = mock(MongoTemplate.class);
        IndexOperations categories = mock(IndexOperations.class);
        IndexOperations products = mock(IndexOperations.class);
        when(template.indexOps(CatalogCategoryDocument.class)).thenReturn(categories);
        when(template.indexOps(ProductoMongoDocument.class)).thenReturn(products);

        new CategoryIndexInitializer(template).run(null);

        var categoryIndex = ArgumentCaptor.forClass(IndexDefinition.class);
        var productIndex = ArgumentCaptor.forClass(IndexDefinition.class);
        verify(categories).ensureIndex(categoryIndex.capture());
        verify(products).ensureIndex(productIndex.capture());
        assertThat(categoryIndex.getValue().getIndexKeys()).containsEntry("name", 1);
        assertThat(categoryIndex.getValue().getIndexOptions()).containsEntry("unique", true);
        assertThat(productIndex.getValue().getIndexKeys()).containsEntry("categoryId", 1);
    }
}

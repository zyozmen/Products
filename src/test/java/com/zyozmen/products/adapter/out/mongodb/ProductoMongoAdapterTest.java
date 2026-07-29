package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.CategoryDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProductoMongoDocument;
import com.zyozmen.products.adapter.out.mongodb.mapper.ProductoMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.ProductoMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.sequence.SequenceGeneratorService;
import com.zyozmen.products.domain.model.Category;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.model.Ranking;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoMongoAdapterTest {

    @Mock
    private ProductoMongoRepository mongoRepository;

    @Mock
    private ProductoMongoMapper mapper;

    @Mock
    private SequenceGeneratorService sequenceGenerator;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ProductoMongoAdapter adapter;

    @Test
    void findAllShouldReturnMappedProducts() {
        ProductoMongoDocument document = ProductoMongoDocument.builder().id("1").name("Headphones").build();
        Producto producto = Producto.builder().id("1").name("Headphones").build();

        when(mongoRepository.findAll()).thenReturn(List.of(document));
        when(mapper.toDomain(document)).thenReturn(producto);

        List<Producto> result = adapter.findAll();

        assertThat(result).containsExactly(producto);
        verify(mapper).toDomain(document);
    }

    @Test
    void findByIdShouldReturnMappedProductWhenPresent() {
        ProductoMongoDocument document = ProductoMongoDocument.builder().id("1").name("Headphones").build();
        Producto producto = Producto.builder().id("1").name("Headphones").build();

        when(mongoRepository.findById("1")).thenReturn(Optional.of(document));
        when(mapper.toDomain(document)).thenReturn(producto);

        Optional<Producto> result = adapter.findById(1L);

        assertThat(result).contains(producto);
        verify(mapper).toDomain(document);
    }

    @Test
    void findAllShouldUsePagingForPagedRequests() {
        Pageable pageable = PageRequest.of(0, 10);
        ProductoMongoDocument document = ProductoMongoDocument.builder().id("2").name("Keyboard").build();
        Producto producto = Producto.builder().id("2").name("Keyboard").build();
        Page<ProductoMongoDocument> page = new PageImpl<>(List.of(document), pageable, 1);

        when(mongoRepository.findAll(pageable)).thenReturn(page);
        when(mapper.toDomain(document)).thenReturn(producto);

        Page<Producto> result = adapter.findAll(pageable);

        assertThat(result.getContent()).containsExactly(producto);
        verify(mapper).toDomain(document);
    }

    @Test
    void findAllByCategoryIdsShouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        ProductoMongoDocument document = ProductoMongoDocument.builder().id("3").name("Mouse").build();
        Producto producto = Producto.builder().id("3").name("Mouse").build();
        Page<ProductoMongoDocument> page = new PageImpl<>(List.of(document), pageable, 1);

        when(mongoRepository.findByCategoriesCategoryIdIn(List.of(1L, 2L), pageable)).thenReturn(page);
        when(mapper.toDomain(document)).thenReturn(producto);

        Page<Producto> result = adapter.findAllByCategoryIds(List.of(1L, 2L), pageable);

        assertThat(result.getContent()).containsExactly(producto);
        verify(mapper).toDomain(document);
    }

    @Test
    void findAllFilteredShouldPopulateQueryAndMapContent() {
        Pageable pageable = PageRequest.of(0, 5);
        ProductoMongoDocument document = ProductoMongoDocument.builder().id("4").name("Headphones").build();
        Producto producto = Producto.builder().id("4").name("Headphones").build();

        when(mongoTemplate.count(any(Query.class), eq(ProductoMongoDocument.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(ProductoMongoDocument.class))).thenReturn(List.of(document));
        when(mapper.toDomain(document)).thenReturn(producto);

        Page<Producto> result = adapter.findAllFiltered(
                List.of(1L, 2L),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(500),
                BigDecimal.valueOf(4.0),
                BigDecimal.valueOf(5.0),
                "head",
                pageable);

        assertThat(result.getContent()).containsExactly(producto);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        verify(mongoTemplate).count(any(Query.class), eq(ProductoMongoDocument.class));
        verify(mongoTemplate).find(any(Query.class), eq(ProductoMongoDocument.class));
    }

    @Test
    void findByIdShouldReturnEmptyWhenRepositoryHasNoMatch() {
        when(mongoRepository.findById("5")).thenReturn(Optional.empty());

        Optional<Producto> result = adapter.findById(5L);

        assertThat(result).isEmpty();
    }

    @Test
    void saveShouldGenerateIdWhenMissingAndPersist() {
        Producto producto = Producto.builder().name("Watch").build();
        ProductoMongoDocument document = ProductoMongoDocument.builder().id("6").name("Watch").build();
        Producto savedProduct = Producto.builder().id("6").name("Watch").build();

        when(sequenceGenerator.nextSequence(SequenceGeneratorService.PRODUCTO_SEQUENCE)).thenReturn(6L);
        when(mapper.toDocument(producto)).thenReturn(document);
        when(mongoRepository.save(document)).thenReturn(document);
        when(mapper.toDomain(document)).thenReturn(savedProduct);

        Producto result = adapter.save(producto);

        assertThat(result).isEqualTo(savedProduct);
        assertThat(producto.getId()).isEqualTo("6");
        verify(sequenceGenerator).nextSequence(SequenceGeneratorService.PRODUCTO_SEQUENCE);
    }

    @Test
    void saveShouldReuseExistingMongoIdWhenUpdating() {
        Producto producto = Producto.builder().id("7").name("Tablet").build();
        ProductoMongoDocument existingDocument = ProductoMongoDocument.builder().id("mongo-7").name("Old").build();
        ProductoMongoDocument updatedDocument = ProductoMongoDocument.builder().id("mongo-7").name("Tablet").build();
        Producto savedProduct = Producto.builder().id("7").name("Tablet").build();

        when(mongoRepository.findById("7")).thenReturn(Optional.of(existingDocument));
        when(mapper.toDocument(producto, "mongo-7")).thenReturn(updatedDocument);
        when(mongoRepository.save(updatedDocument)).thenReturn(updatedDocument);
        when(mapper.toDomain(updatedDocument)).thenReturn(savedProduct);

        Producto result = adapter.save(producto);

        assertThat(result).isEqualTo(savedProduct);
        verify(mongoRepository).findById("7");
        verify(mongoRepository).save(updatedDocument);
    }

    @Test
    void existsAndDeleteShouldDelegateToRepository() {
        when(mongoRepository.existsById("8")).thenReturn(true);

        assertThat(adapter.existsById(8L)).isTrue();
        adapter.deleteById(8L);

        verify(mongoRepository).existsById("8");
        verify(mongoRepository).deleteById("8");
    }

    @Test
    void findFeaturedShouldReturnTopRatedProductsLimitedToTen() {
        ProductoMongoDocument featuredDocument = ProductoMongoDocument.builder().id("9").name("Console").ranking(new com.zyozmen.products.adapter.out.mongodb.document.RankingDocument(BigDecimal.valueOf(4.8), 20, null)).build();
        ProductoMongoDocument lowRatedDocument = ProductoMongoDocument.builder().id("10").name("Low").ranking(new com.zyozmen.products.adapter.out.mongodb.document.RankingDocument(BigDecimal.valueOf(3.5), 5, null)).build();
        Producto featuredProduct = Producto.builder().id("9").name("Console").ranking(Ranking.builder().averageRating(BigDecimal.valueOf(4.8)).build()).build();
        Producto lowRatedProduct = Producto.builder().id("10").name("Low").ranking(Ranking.builder().averageRating(BigDecimal.valueOf(3.5)).build()).build();

        when(mongoRepository.findAll()).thenReturn(List.of(featuredDocument, lowRatedDocument));
        when(mapper.toDomain(featuredDocument)).thenReturn(featuredProduct);
        when(mapper.toDomain(lowRatedDocument)).thenReturn(lowRatedProduct);

        List<Producto> result = adapter.findFeatured();

        assertThat(result).containsExactly(featuredProduct);
    }

    @Test
    void findAllFilteredShouldSupportPriceOnlyFilters() {
        Pageable pageable = PageRequest.of(0, 2);
        ProductoMongoDocument document = ProductoMongoDocument.builder().id("11").name("Speaker").build();
        Producto producto = Producto.builder().id("11").name("Speaker").build();

        when(mongoTemplate.count(any(Query.class), eq(ProductoMongoDocument.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(ProductoMongoDocument.class))).thenReturn(List.of(document));
        when(mapper.toDomain(document)).thenReturn(producto);

        Page<Producto> result = adapter.findAllFiltered(null, BigDecimal.TEN, BigDecimal.valueOf(50), null, null, null, pageable);

        assertThat(result.getContent()).containsExactly(producto);
    }

    @Test
    void findAllFilteredShouldSupportRatingOnlyFilters() {
        Pageable pageable = PageRequest.of(0, 2);
        ProductoMongoDocument document = ProductoMongoDocument.builder().id("12").name("Camera").build();
        Producto producto = Producto.builder().id("12").name("Camera").build();

        when(mongoTemplate.count(any(Query.class), eq(ProductoMongoDocument.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(ProductoMongoDocument.class))).thenReturn(List.of(document));
        when(mapper.toDomain(document)).thenReturn(producto);

        Page<Producto> result = adapter.findAllFiltered(null, null, null, BigDecimal.valueOf(4.0), BigDecimal.valueOf(5.0), null, pageable);

        assertThat(result.getContent()).containsExactly(producto);
    }

    @Test
    void findAllFilteredShouldSupportNameOnlyFilters() {
        Pageable pageable = PageRequest.of(0, 2);
        ProductoMongoDocument document = ProductoMongoDocument.builder().id("13").name("Headphones").build();
        Producto producto = Producto.builder().id("13").name("Headphones").build();

        when(mongoTemplate.count(any(Query.class), eq(ProductoMongoDocument.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(ProductoMongoDocument.class))).thenReturn(List.of(document));
        when(mapper.toDomain(document)).thenReturn(producto);

        Page<Producto> result = adapter.findAllFiltered(null, null, null, null, null, "head", pageable);

        assertThat(result.getContent()).containsExactly(producto);
    }

    @Test
    void findAllFilteredShouldHandleEmptyCriteriaWithoutErrors() {
        Pageable pageable = PageRequest.of(0, 2);
        ProductoMongoDocument document = ProductoMongoDocument.builder().id("14").name("Keyboard").build();
        Producto producto = Producto.builder().id("14").name("Keyboard").build();

        when(mongoTemplate.count(any(Query.class), eq(ProductoMongoDocument.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(ProductoMongoDocument.class))).thenReturn(List.of(document));
        when(mapper.toDomain(document)).thenReturn(producto);

        Page<Producto> result = adapter.findAllFiltered(null, null, null, null, null, null, pageable);

        assertThat(result.getContent()).containsExactly(producto);
    }

    @Test
    void saveShouldUseFallbackDocumentPathWhenExistingProductIsMissing() {
        Producto producto = Producto.builder().id("15").name("Tablet").build();
        ProductoMongoDocument fallbackDocument = ProductoMongoDocument.builder().id("fallback-15").name("Tablet").build();
        Producto savedProduct = Producto.builder().id("15").name("Tablet").build();

        when(mongoRepository.findById("15")).thenReturn(Optional.empty());
        when(mapper.toDocument(producto)).thenReturn(fallbackDocument);
        when(mongoRepository.save(fallbackDocument)).thenReturn(fallbackDocument);
        when(mapper.toDomain(fallbackDocument)).thenReturn(savedProduct);

        Producto result = adapter.save(producto);

        assertThat(result).isEqualTo(savedProduct);
        verify(mongoRepository).save(fallbackDocument);
    }

    @Test
    void findAllCategoriesShouldReturnSortedCategories() {
        CategoryDocument categoryDocument = CategoryDocument.builder().categoryId(2L).name("Electronics").slug("electronics").productsCount(4L).build();
        Category category = Category.builder().categoryId(2L).name("Electronics").slug("electronics").productsCount(4L).build();

        when(mongoRepository.findDistinctCategories()).thenReturn(List.of(categoryDocument));
        when(mapper.toCategoryDomain(categoryDocument)).thenReturn(category);

        List<Category> result = adapter.findAllCategories();

        assertThat(result).containsExactly(category);
        verify(mapper).toCategoryDomain(categoryDocument);
    }

    @Test
    void findAllCategoriesShouldSortNullCategoryIdsLast() {
        CategoryDocument first = CategoryDocument.builder().categoryId(null).name("Unsorted").slug("unsorted").productsCount(1L).build();
        CategoryDocument second = CategoryDocument.builder().categoryId(2L).name("Electronics").slug("electronics").productsCount(4L).build();
        Category firstCategory = Category.builder().categoryId(null).name("Unsorted").slug("unsorted").productsCount(1L).build();
        Category secondCategory = Category.builder().categoryId(2L).name("Electronics").slug("electronics").productsCount(4L).build();

        when(mongoRepository.findDistinctCategories()).thenReturn(List.of(second, first));
        when(mapper.toCategoryDomain(second)).thenReturn(secondCategory);
        when(mapper.toCategoryDomain(first)).thenReturn(firstCategory);

        List<Category> result = adapter.findAllCategories();

        assertThat(result).containsExactly(secondCategory, firstCategory);
    }
}

package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.adapter.out.mongodb.ProductoMongoAdapter;
import com.zyozmen.products.adapter.out.mongodb.document.ProductoMongoDocument;
import com.zyozmen.products.adapter.out.mongodb.mapper.ProductoMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.ProductoMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.sequence.SequenceGeneratorService;
import com.zyozmen.products.domain.model.Producto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoRepositoryPortTest {
    @Mock
    private ProductoMongoRepository productoMongoRepository;

    @Mock
    private ProductoMongoMapper productoMongoMapper;

    @Mock
    private SequenceGeneratorService sequenceGenerator;

    @Mock
    private MongoTemplate mongoTemplate;

    @Test
    void shouldExposeContractMethodsForRepositoryOperations() {
        ProductoRepositoryPort port = new ProductoMongoAdapter(productoMongoRepository, productoMongoMapper, sequenceGenerator, mongoTemplate);

        Producto producto = Producto.builder().id("1").build();

        when(productoMongoRepository.findAll()).thenReturn(List.of());
        when(productoMongoRepository.findAll(Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(), Pageable.unpaged(), 0));
        when(productoMongoRepository.findByCategoriesCategoryIdIn(List.of(1L), Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(), Pageable.unpaged(), 0));
        when(productoMongoRepository.findById("1")).thenReturn(Optional.empty());
        when(productoMongoRepository.existsById("1")).thenReturn(false);
        when(productoMongoRepository.findDistinctCategories()).thenReturn(List.of());
        when(productoMongoMapper.toDocument(any(Producto.class))).thenReturn(new ProductoMongoDocument());
        when(productoMongoRepository.save(any(ProductoMongoDocument.class))).thenReturn(new ProductoMongoDocument());
        when(productoMongoMapper.toDomain(any(ProductoMongoDocument.class))).thenReturn(producto);
        when(mongoTemplate.count(any(), eq(ProductoMongoDocument.class))).thenReturn(0L);
        when(mongoTemplate.find(any(), eq(ProductoMongoDocument.class))).thenReturn(List.of());

        assertThat(port.findAll()).isEmpty();
        assertThat(port.findAll(Pageable.unpaged()).getTotalElements()).isZero();
        assertThat(port.findAllByCategoryIds(List.of(1L), Pageable.unpaged()).getTotalElements()).isZero();
        assertThat(port.findAllFiltered(List.of(1L), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, "name", Pageable.unpaged()).getTotalElements()).isZero();
        assertThat(port.findById(1L)).isEmpty();
        assertThat(port.save(producto)).isSameAs(producto);
        assertThat(port.existsById(1L)).isFalse();
        port.deleteById(1L);
        assertThat(port.findFeatured()).isEmpty();
        assertThat(port.findAllCategories()).isEmpty();
    }
}

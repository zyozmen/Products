package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.ProductoMongoDocument;
import com.zyozmen.products.adapter.out.mongodb.mapper.ProductoMongoMapper;
import com.zyozmen.products.adapter.out.mongodb.repository.ProductoMongoRepository;
import com.zyozmen.products.adapter.out.mongodb.sequence.SequenceGeneratorService;
import com.zyozmen.products.domain.model.Producto;
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
}

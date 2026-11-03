package com.zyozmen.products.adapter.out.mongodb;

import com.zyozmen.products.adapter.out.mongodb.document.ImageDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProductDocument;
import com.zyozmen.products.adapter.out.mongodb.repository.ProductMongoRepository;
import com.zyozmen.products.domain.model.Image;
import com.zyozmen.products.domain.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductMongoAdapterTest {

    private final ProductMongoRepository repository = mock(ProductMongoRepository.class);
    private final ProductMongoAdapter adapter = new ProductMongoAdapter(repository);

    @Test
    void savesProductWithNormalizedNameAndMapsItsImage() {
        var now = Instant.parse("2026-11-03T15:00:00Z");
        var product = new Product("cepa-1", " Tangerine ", new Image("photo.png", "png", "AQID"),
                now, now, 2L, List.of(7L));
        var saved = new ProductDocument("cepa-1", " Tangerine ", "tangerine",
                new ImageDocument("photo.png", "png", "AQID"), now, now, 2L, List.of(7L));
        when(repository.save(any(ProductDocument.class))).thenReturn(saved);

        var result = adapter.save(product);

        assertThat(result.nombre()).isEqualTo(" Tangerine ");
        assertThat(result.categoryIds()).containsExactly(7L);
        assertThat(result.fotografia()).isEqualTo(product.fotografia());
        var captor = org.mockito.ArgumentCaptor.forClass(ProductDocument.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().nombreNormalizado()).isEqualTo("tangerine");
    }

    @Test
    void findsProductsByIdAndMapsPages() {
        var document = new ProductDocument("cepa-1", "Tangerine", "tangerine",
                null, null, null, 0L, List.of());
        when(repository.findById("cepa-1")).thenReturn(Optional.of(document));
        when(repository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(document), PageRequest.of(0, 10), 1));

        assertThat(adapter.findById("cepa-1").orElseThrow().id()).isEqualTo("cepa-1");
        assertThat(adapter.findAll(PageRequest.of(0, 10)).getContent()).hasSize(1);
        assertThat(adapter.findById("missing")).isEmpty();
        assertThatThrownBy(() -> adapter.findById(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.findById("  ")).isInstanceOf(IllegalArgumentException.class);
        verify(repository).findAll(any(PageRequest.class));
    }
}

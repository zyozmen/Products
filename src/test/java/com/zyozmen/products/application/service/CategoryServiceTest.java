package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.CatalogCategory;
import com.zyozmen.products.domain.model.Image;
import com.zyozmen.products.domain.port.out.CategoryRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock
    private CategoryRepositoryPort repository;
    @InjectMocks
    private CategoryService service;

    @Test
    void createsCategoryIndependentlyAndSetsUtcTimestamps() {
        var category = CatalogCategory.builder().categoryId(99L).name("  Audio  ").description("Speakers").build();
        when(repository.save(category)).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.createCategory(category)).isSameAs(category);
        assertThat(category.getCategoryId()).isNull();
        assertThat(category.getName()).isEqualTo("Audio");
        assertThat(category.getCreatedAt()).isNotNull().isEqualTo(category.getUpdatedAt());
    }

    @Test
    void updatesCategoryPreservingIdentityCreatedAtAndImage() {
        var created = LocalDateTime.of(2026, 1, 1, 0, 0);
        var image = new Image("test.png", "png", "AQID");
        var existing = CatalogCategory.builder().categoryId(1L).name("Old").createdAt(created)
                .image(image).imageUrl("/api/v1/categories/1/image").build();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        var result = service.updateCategory(1L, CatalogCategory.builder().name(" New ").description("Updated").build());

        assertThat(result.getCategoryId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getDescription()).isEqualTo("Updated");
        assertThat(result.getCreatedAt()).isEqualTo(created);
        assertThat(result.getImage()).isSameAs(image);
        assertThat(result.getImageUrl()).isEqualTo("/api/v1/categories/1/image");
        assertThat(result.getUpdatedAt()).isAfter(created);
    }

    @Test
    void updateWithExternalUrlRemovesStoredImage() {
        var existing = CatalogCategory.builder().categoryId(1L).image(new Image()).build();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        var result = service.updateCategory(1L, CatalogCategory.builder().name("Audio")
                .imageUrl("https://example.com/audio.png").build());

        assertThat(result.getImageUrl()).isEqualTo("https://example.com/audio.png");
        assertThat(result.getImage()).isNull();
    }

    @Test
    void listIncludesCategoriesWithoutProducts() {
        var category = CatalogCategory.builder().categoryId(1L).name("Audio").build();
        when(repository.findAll()).thenReturn(List.of(category));
        when(repository.findById(1L)).thenReturn(Optional.of(category));

        assertThat(service.getAllCategories()).containsExactly(category);
        assertThat(service.getCategoryById(1L)).isSameAs(category);
    }

    @Test
    void missingCategoryReturnsNotFoundAndIsNotSaved() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCategory(99L, CatalogCategory.builder().name("Audio").build()))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("99");
        verify(repository, never()).save(any());
    }

    @Test
    void savesMainImageAndExposesPublicUrl() {
        var category = CatalogCategory.builder().categoryId(2L).name("Audio").build();
        var image = new Image("audio.png", "png", "AQID");
        when(repository.findById(2L)).thenReturn(Optional.of(category));
        when(repository.save(category)).thenReturn(category);

        var result = service.saveImage(2L, image);

        assertThat(result.getImage()).isSameAs(image);
        assertThat(result.getImageUrl()).isEqualTo("/api/v1/categories/2/image");
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void categoryImageIsRequired() {
        assertThatThrownBy(() -> service.saveImage(1L, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("principal");
        verifyNoInteractions(repository);
    }
}

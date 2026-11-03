package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.CategoryRequestDTO;
import com.zyozmen.products.adapter.in.web.mapper.CategoryWebMapper;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.CatalogCategory;
import com.zyozmen.products.domain.model.Image;
import com.zyozmen.products.domain.port.in.CategoryUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryControllerTest {

    private final CategoryUseCase useCase = mock(CategoryUseCase.class);
    private final CategoryController controller = new CategoryController(useCase, new CategoryWebMapper());

    @Test
    void delegatesCategoryCrudAndMapsResponses() {
        var request = new CategoryRequestDTO("Flores", "Productos florales", null);
        var category = CatalogCategory.builder().categoryId(7L).name("Flores")
                .description("Productos florales").build();
        when(useCase.createCategory(org.mockito.ArgumentMatchers.any())).thenReturn(category);
        when(useCase.updateCategory(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(category);
        when(useCase.getAllCategories()).thenReturn(List.of(category));
        when(useCase.getCategoryById(7L)).thenReturn(category);

        assertThat(controller.create(request).getBody().name()).isEqualTo("Flores");
        assertThat(controller.update(7L, request).getBody().id()).isEqualTo(7L);
        assertThat(controller.findAll()).hasSize(1);
        assertThat(controller.findById(7L).name()).isEqualTo("Flores");

        verify(useCase).createCategory(org.mockito.ArgumentMatchers.argThat(
                value -> "Flores".equals(value.getName()) && "Productos florales".equals(value.getDescription())));
        verify(useCase).updateCategory(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any());
    }

    @ParameterizedTest
    @CsvSource({"png,image/png", "jpg,image/jpeg", "jpeg,image/jpeg", "webp,image/webp"})
    void servesCategoryImageWithMatchingContentType(String extension, String contentType) {
        var category = CatalogCategory.builder().categoryId(7L)
                .image(new Image("photo." + extension, extension, Base64.getEncoder().encodeToString(new byte[]{1, 2})))
                .build();
        when(useCase.getCategoryById(7L)).thenReturn(category);

        var response = controller.image(7L);

        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType(contentType));
        assertThat(response.getBody()).containsExactly((byte) 1, (byte) 2);
    }

    @Test
    void rejectsMissingOrUnsupportedCategoryImages() {
        when(useCase.getCategoryById(7L)).thenReturn(CatalogCategory.builder().categoryId(7L).build());
        assertThatThrownBy(() -> controller.image(7L)).isInstanceOf(ResourceNotFoundException.class);

        when(useCase.getCategoryById(8L)).thenReturn(CatalogCategory.builder().categoryId(8L)
                .image(new Image("photo.gif", "gif", "AQ==")).build());
        assertThatThrownBy(() -> controller.image(8L)).isInstanceOf(IllegalStateException.class);
    }
}

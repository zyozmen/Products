package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.CategoryResponseDTO;
import com.zyozmen.products.adapter.in.web.mapper.CategoryWebMapper;
import com.zyozmen.products.domain.model.CatalogCategory;
import com.zyozmen.products.domain.model.Product;
import com.zyozmen.products.domain.port.in.CategoryUseCase;
import com.zyozmen.products.domain.port.in.ProductUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImageControllerTest {

    @Test
    void uploadsProductAndCategoryImagesToTheirUseCases() {
        ImageUploadService uploadService = new ImageUploadService();
        CategoryUseCase categoryUseCase = mock(CategoryUseCase.class);
        ProductUseCase productUseCase = mock(ProductUseCase.class);
        ImageController controller = new ImageController(uploadService, categoryUseCase,
                new CategoryWebMapper(), productUseCase);
        var file = new MockMultipartFile("fotoPrincipal", "photo.png", "image/png", new byte[]{1});
        Product product = new Product("product-1", "Tangerine");
        when(productUseCase.guardarFotografia(eq("product-1"), any())).thenReturn(product);
        when(categoryUseCase.saveImage(eq(7L), any()))
                .thenReturn(CatalogCategory.builder().categoryId(7L).name("Flores").build());

        assertThat(controller.upload("product-1", ImageUploadService.Target.PRODUCT, file).getBody())
                .isSameAs(product);
        assertThat(controller.upload("7", ImageUploadService.Target.CATEGORY, file).getBody())
                .isEqualTo(new CategoryResponseDTO(7L, "Flores", null, null));
    }

    @Test
    void rejectsNonNumericCategoryImageIds() {
        ImageController controller = new ImageController(new ImageUploadService(), mock(CategoryUseCase.class),
                new CategoryWebMapper(), mock(ProductUseCase.class));
        var file = new MockMultipartFile("fotoPrincipal", "photo.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> controller.upload("not-a-number", ImageUploadService.Target.CATEGORY, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("numerico");
    }
}

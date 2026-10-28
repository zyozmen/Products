package com.zyozmen.products.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.zyozmen.products.adapter.in.web.ImageUploadService.Target.CATEGORY;
import static com.zyozmen.products.adapter.in.web.ImageUploadService.Target.PRODUCT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ImageUploadServiceTest {
    private final ImageUploadService service = new ImageUploadService();

    @ParameterizedTest
    @CsvSource({"photo.png,image/png,png", "photo.JPG,image/jpeg,jpg", "photo.jpeg,image/jpeg,jpeg", "photo.webp,image/webp,webp"})
    void acceptsSupportedFormats(String name, String contentType, String extension) {
        var file = new MockMultipartFile("fotoPrincipal", name, contentType, new byte[]{1, 2, 3});

        var images = service.prepare(PRODUCT, file);

        assertThat(images.getFotoPrincipal().getNombre()).isEqualTo(name);
        assertThat(images.getFotoPrincipal().getExtension()).isEqualTo(extension);
        assertThat(images.getFotoPrincipal().getFilepart()).isEqualTo("AQID");
    }

    @Test
    void categoryIgnoresSecondaryFilesWithoutReadingOrValidatingThem() {
        var main = new MockMultipartFile("fotoPrincipal", "photo.png", "image/png", new byte[]{1});
        MultipartFile invalidSecondary = mock(MultipartFile.class);

        var images = service.prepare(CATEGORY, main);

        assertThat(images.getFotoPrincipal()).isNotNull();
        verifyNoInteractions(invalidSecondary);
    }

    @ParameterizedTest
    @CsvSource({"image.gif,image/gif", "image.svg,image/svg+xml", "image.png,application/pdf", "image, image/png"})
    void rejectsUnsupportedExtensionOrMismatchedContentType(String name, String contentType) {
        var file = new MockMultipartFile("fotoPrincipal", name, contentType, new byte[]{1});
        assertThatThrownBy(() -> service.prepare(PRODUCT, file)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsExactlyTwoMiBAndRejectsOneByteMore() {
        var limit = new MockMultipartFile("fotoPrincipal", "photo.png", "image/png",
                new byte[(int) ImageUploadService.MAX_SIZE]);
        var oversized = new MockMultipartFile("fotoPrincipal", "photo.png", "image/png",
                new byte[(int) ImageUploadService.MAX_SIZE + 1]);

        assertThat(service.prepare(CATEGORY, limit).getFotoPrincipal()).isNotNull();
        assertThatThrownBy(() -> service.prepare(CATEGORY, oversized))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("2 MB");
    }
}

package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.mapper.CategoryWebMapper;
import com.zyozmen.products.domain.model.Images;
import com.zyozmen.products.domain.port.in.CategoryUseCase;
import com.zyozmen.products.domain.port.in.ProductUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {
    private final ImageUploadService imageUploadService;
    private final CategoryUseCase categoryUseCase;
    private final CategoryWebMapper categoryMapper;
    private final ProductUseCase productoUseCase;

    @PostMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@PathVariable String id,
            @RequestParam ImageUploadService.Target target,
            @RequestParam(required = false) MultipartFile fotoPrincipal) {
        Images images = imageUploadService.prepare(target, fotoPrincipal);
        if (target == ImageUploadService.Target.PRODUCT) {
            return ResponseEntity.ok(productoUseCase.guardarFotografia(id, images.getFotoPrincipal()));
        }
        long numericId;
        try {
            numericId = Long.parseLong(id);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("El ID de producto o categoria debe ser numerico", exception);
        }
        if (target == ImageUploadService.Target.CATEGORY) {
            return ResponseEntity.ok(categoryMapper.toResponse(categoryUseCase.saveImage(numericId, images.getFotoPrincipal())));
        }
        throw new IllegalArgumentException("Destino de imagen no soportado");
    }
}

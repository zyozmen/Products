package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.CategoryRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.CategoryResponseDTO;
import com.zyozmen.products.adapter.in.web.mapper.CategoryWebMapper;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.Image;
import com.zyozmen.products.domain.port.in.CategoryUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryUseCase categoryUseCase;
    private final CategoryWebMapper mapper;

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> create(@Valid @RequestBody CategoryRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponse(categoryUseCase.createCategory(mapper.toDomain(request))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> update(@PathVariable Long id, @Valid @RequestBody CategoryRequestDTO request) {
        return ResponseEntity.ok(mapper.toResponse(categoryUseCase.updateCategory(id, mapper.toDomain(request))));
    }

    @GetMapping
    public List<CategoryResponseDTO> findAll() {
        return categoryUseCase.getAllCategories().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public CategoryResponseDTO findById(@PathVariable Long id) {
        return mapper.toResponse(categoryUseCase.getCategoryById(id));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(@PathVariable Long id) {
        Image image = categoryUseCase.getCategoryById(id).getImage();
        if (image == null) {
            throw new ResourceNotFoundException("La categoria no tiene una imagen almacenada");
        }
        String contentType = switch (image.getExtension()) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            default -> throw new IllegalStateException("Formato de imagen almacenada no soportado");
        };
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                .body(Base64.getDecoder().decode(image.getFilepart()));
    }
}

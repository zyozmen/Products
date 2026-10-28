package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.domain.model.Product;
import com.zyozmen.products.domain.port.in.ProductUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Administracion del catalogo de productos")
public class ProductController {
    private final ProductUseCase useCase;

    public record ProductRequest(@NotBlank @Size(max = 100) String nombre, Long version,
                              @Size(max = 100) List<@NotNull @Positive Long> categoryIds) { }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product crear(@Valid @RequestBody ProductRequest request) {
        return useCase.crear(request.nombre(), request.categoryIds());
    }

    @PutMapping("/{id}")
    public Product actualizar(@PathVariable String id, @Valid @RequestBody ProductRequest request) {
        return useCase.actualizar(id, request.nombre(), request.version(), request.categoryIds());
    }

    @GetMapping("/{id}")
    public Product obtener(@PathVariable String id) {
        return useCase.obtener(id);
    }

    @GetMapping
    public org.springframework.http.ResponseEntity<List<Product>> listar(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = useCase.listar(page, size);
        return org.springframework.http.ResponseEntity.ok()
                .header("X-Total-Count", Long.toString(result.getTotalElements())).body(result.getContent());
    }
}

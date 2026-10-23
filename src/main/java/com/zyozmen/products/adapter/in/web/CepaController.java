package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.domain.model.Cepa;
import com.zyozmen.products.domain.port.in.CepaUseCase;
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
@RequestMapping("/api/cepas")
@RequiredArgsConstructor
@Tag(name = "Cepas", description = "Administracion del catalogo de cepas")
public class CepaController {
    private final CepaUseCase useCase;

    public record CepaRequest(@NotBlank @Size(max = 100) String nombre, Long version,
                              @Size(max = 100) List<@NotNull @Positive Long> categoryIds) { }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Cepa crear(@Valid @RequestBody CepaRequest request) {
        return useCase.crear(request.nombre(), request.categoryIds());
    }

    @PutMapping("/{id}")
    public Cepa actualizar(@PathVariable String id, @Valid @RequestBody CepaRequest request) {
        return useCase.actualizar(id, request.nombre(), request.version(), request.categoryIds());
    }

    @GetMapping("/{id}")
    public Cepa obtener(@PathVariable String id) {
        return useCase.obtener(id);
    }

    @GetMapping
    public org.springframework.http.ResponseEntity<List<Cepa>> listar(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = useCase.listar(page, size);
        return org.springframework.http.ResponseEntity.ok()
                .header("X-Total-Count", Long.toString(result.getTotalElements())).body(result.getContent());
    }
}

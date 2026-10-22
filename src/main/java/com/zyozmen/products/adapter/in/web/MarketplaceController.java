package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.domain.port.in.MarketplaceUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Marketplace", description = "Productos por cepa y presentaciones")
public class MarketplaceController {
    private final MarketplaceUseCase marketplace;

    public record PaginaCatalogo(List<MarketplaceUseCase.ProductoCatalogo> content,
                                 int page, int size, long totalElements, int totalPages) { }

    @GetMapping("/api/marketplace/presentaciones")
    @Operation(summary = "Catalogo paginado por cepas con presentaciones activas y disponibles")
    public PaginaCatalogo listar(@RequestParam(required = false) String buscar,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        var resultado = marketplace.listar(buscar, page, size);
        return new PaginaCatalogo(resultado.getContent(), page, size,
                resultado.getTotalElements(), resultado.getTotalPages());
    }

    @GetMapping("/api/marketplace/cepas/{cepaId}/presentaciones/{presentacionId}")
    @Operation(summary = "Consultar una cepa con sus presentaciones activas de distintos lotes")
    public MarketplaceUseCase.DetalleProducto obtenerDetalle(
            @PathVariable String cepaId, @PathVariable String presentacionId) {
        return marketplace.obtenerDetalle(cepaId, presentacionId);
    }
}

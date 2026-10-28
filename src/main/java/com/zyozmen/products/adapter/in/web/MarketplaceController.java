package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.domain.port.in.MarketplaceUseCase;
import com.zyozmen.products.domain.model.MarketplaceCatalogFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@Tag(name = "Marketplace", description = "Productos por cepa y presentaciones")
public class MarketplaceController {
    private final MarketplaceUseCase marketplace;

    public record PaginaCatalogo(List<MarketplaceUseCase.ProductoCatalogo> content,
                                 int page, int size, long totalElements, int totalPages) { }

    @GetMapping("/api/marketplace/presentaciones")
    @Operation(summary = "Catalogo paginado por productos con presentaciones activas y disponibles")
    public PaginaCatalogo listar(@RequestParam(required = false) String buscar,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 @RequestParam(required = false) List<Long> categoryIds,
                                 @RequestParam(required = false) BigDecimal minPrice,
                                 @RequestParam(required = false) BigDecimal maxPrice) {
        var resultado = marketplace.listar(new MarketplaceCatalogFilter(buscar, categoryIds, minPrice, maxPrice), page, size);
        return new PaginaCatalogo(resultado.getContent(), page, size,
                resultado.getTotalElements(), resultado.getTotalPages());
    }

    @GetMapping("/api/marketplace/products/{productId}/presentaciones/{presentacionId}")
    @Operation(summary = "Consultar una cepa con sus presentaciones activas de distintos lotes")
    public MarketplaceUseCase.DetalleProducto obtenerDetalle(
            @PathVariable String productId, @PathVariable String presentacionId) {
        return marketplace.obtenerDetalle(productId, presentacionId);
    }
}

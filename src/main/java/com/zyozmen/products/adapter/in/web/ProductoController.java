package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.port.in.ProductoUseCase;
import com.zyozmen.products.adapter.in.web.dto.CategoryDTO;
import com.zyozmen.products.adapter.in.web.dto.ProductoListItemDTO;
import com.zyozmen.products.adapter.in.web.dto.ProductoRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.ProductoResponseDTO;
import com.zyozmen.products.adapter.in.web.mapper.ProductoWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Adaptador de entrada REST (Inbound Adapter).
 *
 * Responsabilidades:
 *   1. Recibir la petición HTTP y deserializar el DTO de entrada.
 *   2. Mapear el DTO al modelo de dominio usando ProductoWebMapper.
 *   3. Delegar al puerto de entrada (ProductoUseCase).
 *   4. Mapear el resultado de dominio al DTO de respuesta.
 *
 * No contiene lógica de negocio. Depende del puerto (interfaz),
 * nunca de la implementación concreta del servicio.
 */
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Permitir CORS para todas las fuentes (ajustar según necesidades)
@Tag(name = "Productos", description = "API para gestión de productos")
public class ProductoController {

    private final ProductoUseCase productoUseCase;
    private final ProductoWebMapper productoWebMapper;

    @Operation(summary = "Listar todos los productos")
    @ApiResponse(responseCode = "200", description = "Lista de productos obtenida exitosamente")
    @GetMapping
    public ResponseEntity<Page<ProductoListItemDTO>> listarTodos(
            @Parameter(description = "Número de página (base 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página", example = "10")
            @RequestParam(defaultValue = "15") int size,
            @Parameter(description = "Campo para ordenar (price|rating)", example = "price")
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @Parameter(description = "Dirección de orden (asc|desc)", example = "asc")
            @RequestParam(name = "sort_dir", defaultValue = "desc") String sortDir,
            @Parameter(description = "Precio mínimo (filtro)", example = "100")
            @RequestParam(name = "min_price", required = false) BigDecimal minPrice,
            @Parameter(description = "Precio máximo (filtro)", example = "500")
            @RequestParam(name = "max_price", required = false) BigDecimal maxPrice,
            @Parameter(description = "Calificación mínima (filtro)", example = "4.0")
            @RequestParam(name = "min_rating", required = false) BigDecimal minRating,
            @Parameter(description = "Calificación máxima (filtro)", example = "5.0")
            @RequestParam(name = "max_rating", required = false) BigDecimal maxRating,
            @Parameter(description = "Nombre del producto (filtro parcial, case-insensitive)", example = "headphone")
            @RequestParam(name = "name", required = false) String name,
            @Parameter(description = "Filtrar por múltiples IDs de categoría", example = "1,2,3")
            @RequestParam(name = "category_ids", required = false) List<Long> categoryIds) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = switch (sortBy == null ? "" : sortBy.toLowerCase()) {
            case "price" -> Sort.by(direction, "price.current");
            case "rating", "average_rating", "calificacion" -> Sort.by(direction, "ranking.averageRating");
            default -> Sort.unsorted();
        };
        Pageable pageable = PageRequest.of(page, size, sort);

        List<Long> effectiveCategoryIds = new ArrayList<>();
        if (categoryIds != null) {
            effectiveCategoryIds.addAll(categoryIds);
        }

        List<Long> normalizedCategoryIds = effectiveCategoryIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

            boolean hasPriceFilter = minPrice != null || maxPrice != null;
            boolean hasRatingFilter = minRating != null || maxRating != null;
            boolean hasNameFilter = name != null && !name.isBlank();

            Page<ProductoListItemDTO> response = ((normalizedCategoryIds.isEmpty() && !hasPriceFilter && !hasRatingFilter && !hasNameFilter)
                ? productoUseCase.listarTodos(pageable)
                : (!hasPriceFilter && !hasRatingFilter && !hasNameFilter)
                    ? productoUseCase.listarTodosPorCategorias(normalizedCategoryIds, pageable)
                    : productoUseCase.listarTodosFiltrado(
                        normalizedCategoryIds,
                        minPrice,
                        maxPrice,
                        minRating,
                        maxRating,
                        name,
                        pageable))
                .map(productoWebMapper::toListItemDTO);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Obtener un producto por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Producto encontrado"),
        @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                     content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> obtenerPorId(
            @Parameter(description = "ID del producto", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(productoWebMapper.toResponseDTO(productoUseCase.obtenerPorId(id)));
    }

    @Operation(summary = "Crear un nuevo producto")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Producto creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                     content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping
    public ResponseEntity<ProductoResponseDTO> crear(@Valid @RequestBody ProductoRequestDTO requestDTO) {
        Producto creado = productoUseCase.crear(productoWebMapper.toDomain(requestDTO));
        return ResponseEntity.status(HttpStatus.CREATED).body(productoWebMapper.toResponseDTO(creado));
    }

    @Operation(summary = "Actualizar un producto existente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Producto actualizado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                     content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                     content = @Content(schema = @Schema(hidden = true)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> actualizar(
            @Parameter(description = "ID del producto a actualizar", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequestDTO requestDTO) {
        Producto actualizado = productoUseCase.actualizar(id, productoWebMapper.toDomain(requestDTO));
        return ResponseEntity.ok(productoWebMapper.toResponseDTO(actualizado));
    }

    @Operation(summary = "Eliminar un producto")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Producto eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                     content = @Content(schema = @Schema(hidden = true)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del producto a eliminar", example = "1")
            @PathVariable Long id) {
        productoUseCase.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Listar todos los productos Destacados")
    @ApiResponse(responseCode = "200", description = "Lista de productos obtenida exitosamente")
    @GetMapping("/featured")
    public ResponseEntity<List<ProductoResponseDTO>> obtenerDestacados() {
        List<ProductoResponseDTO> response = productoUseCase.listarDestacados()
                .stream()
                .map(productoWebMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar todas las categorías")
    @ApiResponse(responseCode = "200", description = "Lista de categorías obtenida exitosamente")
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDTO>> obtenerCategorias() {
        List<CategoryDTO> response = productoUseCase.listarCategorias()
                .stream()
                .map(productoWebMapper::toCategoryDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}

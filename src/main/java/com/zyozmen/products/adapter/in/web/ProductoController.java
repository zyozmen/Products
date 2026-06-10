package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.port.in.ProductoUseCase;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ResponseEntity<List<ProductoResponseDTO>> listarTodos() {
        List<ProductoResponseDTO> response = productoUseCase.listarTodos()
                .stream()
                .map(productoWebMapper::toResponseDTO)
                .collect(Collectors.toList());
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
}

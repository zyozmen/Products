package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.LoteResponseDTO;
import com.zyozmen.products.adapter.in.web.dto.PresentacionCreateRequestDTO;
import com.zyozmen.products.domain.port.in.PresentacionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Presentaciones", description = "Empaquetado e inventario de contenedores")
public class PresentacionController {
    private final PresentacionUseCase useCase;

    @PostMapping("/api/v1/lotes/{loteId}/contenedores/{contenedorId}/presentaciones")
    @Operation(summary = "Agregar presentaciones nuevas y sincronizar el marketplace sin modificar deuda")
    public ResponseEntity<LoteResponseDTO> crear(
            @PathVariable String loteId, @PathVariable String contenedorId,
            @Valid @NotEmpty @RequestBody List<@NotNull @Valid PresentacionCreateRequestDTO> presentaciones) {
        return ResponseEntity.status(HttpStatus.CREATED).body(LoteResponseDTO.from(
                useCase.empaquetar(loteId, contenedorId,
                        presentaciones.stream().map(PresentacionCreateRequestDTO::toCommand).toList())));
    }
}

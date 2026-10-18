package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.EvaluacionLiquidacionResponseDTO;
import com.zyozmen.products.adapter.in.web.dto.EventoLiquidacionRequestDTO;
import com.zyozmen.products.domain.port.in.LiquidacionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/liquidaciones")
@RequiredArgsConstructor
@Tag(name = "Liquidaciones", description = "Evaluación de la liquidación diaria a proveedores")
public class LiquidacionController {

    private final LiquidacionUseCase liquidacionUseCase;

    @PostMapping("/procesar-evento")
    @Operation(summary = "Evaluar si una venta/PQR acumula a la liquidación diaria (aplica el tope de pago)")
    public ResponseEntity<EvaluacionLiquidacionResponseDTO> procesarEvento(
            @Valid @RequestBody EventoLiquidacionRequestDTO request) {
        return ResponseEntity.ok(EvaluacionLiquidacionResponseDTO.from(
                liquidacionUseCase.evaluarEvento(new LiquidacionUseCase.EventoLiquidacionCommand(
                        request.proveedorId(), request.loteId(), request.montoLiquidable()))));
    }
}

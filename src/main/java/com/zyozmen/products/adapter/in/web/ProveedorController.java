package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.LoteCreateRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.LoteResponseDTO;
import com.zyozmen.products.adapter.in.web.dto.PagoProveedorRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.ProveedorCreateRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.ProveedorResponseDTO;
import com.zyozmen.products.domain.port.in.ProveedorUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Proveedores", description = "Gestión de proveedores, lotes y saldos")
public class ProveedorController {

    private final ProveedorUseCase proveedorUseCase;

    @PostMapping("/api/proveedores")
    @Operation(summary = "Crear un proveedor con saldos en cero")
    public ResponseEntity<ProveedorResponseDTO> crear(@Valid @RequestBody ProveedorCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ProveedorResponseDTO.from(
                proveedorUseCase.crear(new ProveedorUseCase.CrearProveedorCommand(
                        request.nombre(), request.nit(), request.telefono(), request.email()))));
    }

    @GetMapping("/api/proveedores/{id}")
    @Operation(summary = "Consultar un proveedor y sus saldos")
    public ResponseEntity<ProveedorResponseDTO> obtener(@PathVariable String id) {
        return ResponseEntity.ok(ProveedorResponseDTO.from(proveedorUseCase.obtener(id)));
    }

    @PostMapping("/api/lotes")
    @Operation(summary = "Recibir un lote calculando su costo, con abono inicial opcional")
    public ResponseEntity<LoteResponseDTO> recibirLote(@Valid @RequestBody LoteCreateRequestDTO request) {
        var presentaciones = request.presentaciones().stream()
                .map(p -> new ProveedorUseCase.PresentacionCommand(
                        p.presentacionId(), p.cantidadUnidades(), p.costoUnitario()))
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(LoteResponseDTO.from(
                proveedorUseCase.recibirLote(new ProveedorUseCase.RecibirLoteCommand(
                        request.proveedorId(), request.codigoLote(), presentaciones, request.abonoInicial()))));
    }

    @PostMapping("/api/proveedores/{id}/pagos")
    @Operation(summary = "Registrar un abono posterior al proveedor (a un lote o a los lotes pendientes)")
    public ResponseEntity<ProveedorResponseDTO> registrarPago(
            @PathVariable String id, @Valid @RequestBody PagoProveedorRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ProveedorResponseDTO.from(
                proveedorUseCase.registrarPago(new ProveedorUseCase.RegistrarPagoCommand(
                        id, request.loteId(), request.monto(), request.tipo(), request.observaciones()))));
    }
}

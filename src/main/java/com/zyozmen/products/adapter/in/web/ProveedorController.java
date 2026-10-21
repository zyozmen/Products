package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.LoteCreateRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.AbonoProveedorResponseDTO;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Proveedores", description = "Gestión de proveedores, lotes y saldos")
public class ProveedorController {

    private final ProveedorUseCase proveedorUseCase;

    @GetMapping("/api/proveedores")
    @Operation(summary = "Buscar proveedores por nombre o NIT, o listar con paginacion")
    public ResponseEntity<List<ProveedorResponseDTO>> buscar(
            @RequestParam(required = false) String buscar,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var resultado = proveedorUseCase.buscar(buscar, page, size);
        return ResponseEntity.ok()
                .header("X-Total-Count", Long.toString(resultado.getTotalElements()))
                .body(resultado.getContent().stream().map(ProveedorResponseDTO::from).toList());
    }

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
        var contenedores = request.contenedores().stream()
                .map(c -> new ProveedorUseCase.ContenedorCommand(c.tipoCepa(), c.unidadBase(), c.presentacionId(),
                        c.capacidadUnidadesBase(), c.costoBasePorUnidad()))
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(LoteResponseDTO.from(
                proveedorUseCase.recibirLote(new ProveedorUseCase.RecibirLoteCommand(
                        request.proveedorId(), contenedores, request.abonoInicial()))));
    }

    @GetMapping("/api/lotes/{id}")
    @Operation(summary = "Consultar un lote y las unidades base disponibles de sus contenedores")
    public ResponseEntity<LoteResponseDTO> obtenerLote(@PathVariable String id) {
        return ResponseEntity.ok(LoteResponseDTO.from(proveedorUseCase.obtenerLote(id)));
    }

    @GetMapping("/api/proveedores/{id}/abonos")
    @Operation(summary = "Consultar movimientos de abonos paginados, del mas reciente al mas antiguo")
    public ResponseEntity<List<AbonoProveedorResponseDTO>> listarAbonos(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var resultado = proveedorUseCase.listarAbonos(id, page, size);
        return ResponseEntity.ok()
                .header("X-Total-Count", Long.toString(resultado.getTotalElements()))
                .body(resultado.getContent().stream().map(AbonoProveedorResponseDTO::from).toList());
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

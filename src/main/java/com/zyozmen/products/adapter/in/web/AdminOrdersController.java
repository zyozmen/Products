package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.OrderResponseDTO;
import com.zyozmen.products.adapter.in.web.dto.OrderStatusRequestDTO;
import com.zyozmen.products.domain.model.OrderStatus;
import com.zyozmen.products.domain.port.in.OrderUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Administración de órdenes", description = "Consulta y gestión administrativa de órdenes")
public class AdminOrdersController {
    private final OrderUseCase orderUseCase;

    @GetMapping
    @Operation(summary = "Listar y filtrar órdenes")
    public ResponseEntity<List<OrderResponseDTO>> findAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Pageable pageable = createPageable(page, size);
        Instant fromInstant = parseDate(from, false);
        Instant toInstant = parseDate(to, true);
        if (fromInstant != null && toInstant != null && fromInstant.isAfter(toInstant)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        }
        Page<com.zyozmen.products.domain.model.Order> result = orderUseCase.findAll(
                parseStatus(status), fromInstant, toInstant, username, pageable);
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (pageable != null) {
            response.header("X-Total-Count", Long.toString(result.getTotalElements()));
        }
        return response.body(result.getContent().stream().map(OrderResponseDTO::from).toList());
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Actualizar el estado de una orden")
    public ResponseEntity<OrderResponseDTO> changeStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusRequestDTO request) {
        OrderStatus status = parseStatus(request.status());
        return ResponseEntity.ok(OrderResponseDTO.from(
                orderUseCase.changeStatus(id, status, authentication.getName())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar el detalle administrativo de una orden")
    public ResponseEntity<OrderResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(OrderResponseDTO.from(orderUseCase.findById(id)));
    }

    private Pageable createPageable(Integer page, Integer size) {
        if (page == null && size == null) {
            return null;
        }
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? 20 : size;
        if (pageNumber < 0 || pageSize < 1 || pageSize > 200) {
            throw new IllegalArgumentException("La página debe ser >= 0 y el tamaño debe estar entre 1 y 200");
        }
        return PageRequest.of(pageNumber, pageSize);
    }

    private OrderStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Estado de orden inválido");
        }
    }

    private Instant parseDate(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(value);
            return (endOfDay ? date.plusDays(1) : date).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Las fechas deben tener formato ISO YYYY-MM-DD");
        }
    }
}

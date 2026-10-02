package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.OrderCreateRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.OrderResponseDTO;
import com.zyozmen.products.domain.port.in.OrderUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Órdenes", description = "Creación y consulta de órdenes propias")
public class OrdersController {
    private final OrderUseCase orderUseCase;

    @PostMapping
    @Operation(summary = "Crear una orden con los precios vigentes del catálogo")
    public ResponseEntity<OrderResponseDTO> create(
            Authentication authentication, @Valid @RequestBody OrderCreateRequestDTO request) {
        List<OrderUseCase.OrderItemRequest> items = request.items().stream()
                .map(item -> new OrderUseCase.OrderItemRequest(item.productId(), item.quantity(),
                        item.unitPrice(), item.lineTotal()))
                .toList();
        OrderUseCase.OrderDeliveryRequest delivery = new OrderUseCase.OrderDeliveryRequest(
                valueOrEmpty(request.delivery().recipientName()),
                valueOrEmpty(request.delivery().address()),
                valueOrEmpty(request.delivery().addressComplement()),
                valueOrEmpty(request.delivery().contactPhone()));
        OrderUseCase.OrderAmountsRequest amounts = new OrderUseCase.OrderAmountsRequest(
                request.subtotal(), request.taxAmount(), request.total());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OrderResponseDTO.from(orderUseCase.create(authentication.getName(), items, delivery, amounts)));
    }

    @GetMapping("/mine")
    @Operation(summary = "Listar las órdenes del usuario autenticado")
    public ResponseEntity<List<OrderResponseDTO>> findMine(Authentication authentication) {
        return ResponseEntity.ok(orderUseCase.findMine(authentication.getName()).stream()
                .map(OrderResponseDTO::from).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una orden propia")
    public ResponseEntity<OrderResponseDTO> findOwnedOrder(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(OrderResponseDTO.from(orderUseCase.findOwnedOrder(id, authentication.getName())));
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}

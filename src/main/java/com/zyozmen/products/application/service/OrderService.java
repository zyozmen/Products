package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.OrderConflictException;
import com.zyozmen.products.domain.exception.OrderProductException;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.Order;
import com.zyozmen.products.domain.model.OrderDelivery;
import com.zyozmen.products.domain.model.OrderItem;
import com.zyozmen.products.domain.model.OrderStatus;
import com.zyozmen.products.domain.model.OrderStatusChange;
import com.zyozmen.products.domain.model.Producto;
import com.zyozmen.products.domain.port.in.OrderUseCase;
import com.zyozmen.products.domain.port.out.OrderRepositoryPort;
import com.zyozmen.products.domain.port.out.ProductoRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService implements OrderUseCase {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.16");
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final String ORDER_NOT_FOUND = "Orden no encontrada";

    private final OrderRepositoryPort orderRepository;
    private final ProductoRepositoryPort productoRepository;

    @Override
    public Order create(String username, List<OrderItemRequest> requestedItems, OrderDeliveryRequest delivery,
                       OrderAmountsRequest requestedAmounts) {
        if (requestedItems == null || requestedItems.isEmpty()) {
            throw new IllegalArgumentException("La orden debe contener al menos un producto");
        }
        if (delivery == null || requestedAmounts == null) {
            throw new IllegalArgumentException("Los datos de entrega y los totales son obligatorios");
        }

        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = ZERO;
        for (OrderItemRequest requestedItem : requestedItems) {
            if (requestedItem == null || requestedItem.quantity() <= 0) {
                throw new IllegalArgumentException("La cantidad de cada producto debe ser mayor que cero");
            }
            long productId;
            try {
                productId = Long.parseLong(requestedItem.productId());
            } catch (NumberFormatException exception) {
                throw new OrderProductException("El producto indicado no existe");
            }

            Producto product = productoRepository.findById(productId)
                    .filter(value -> "active".equalsIgnoreCase(value.getStatus()))
                    .orElseThrow(() -> new OrderProductException(
                            "El producto " + requestedItem.productId() + " no existe o no está activo"));
            if (product.getPrice() == null || product.getPrice().getCurrent() == null) {
                throw new OrderProductException("El producto " + requestedItem.productId() + " no tiene precio válido");
            }

            BigDecimal unitPrice = product.getPrice().getCurrent().setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(requestedItem.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            requireSameAmount(requestedItem.unitPrice(), unitPrice, "El precio del producto cambió");
            requireSameAmount(requestedItem.lineTotal(), lineTotal, "El total del producto no coincide");
            items.add(OrderItem.builder()
                    .productId(product.getId())
                    .name(product.getName())
                    .quantity(requestedItem.quantity())
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .build());
            subtotal = subtotal.add(lineTotal);
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
        requireSameAmount(requestedAmounts.subtotal(), subtotal, "El subtotal enviado no coincide");
        requireSameAmount(requestedAmounts.taxAmount(), taxAmount, "El impuesto enviado no coincide");
        requireSameAmount(requestedAmounts.total(), total, "El total enviado no coincide");

        Instant now = Instant.now();
        return orderRepository.save(Order.builder()
                .username(username)
                .status(OrderStatus.CREADA)
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .total(total)
                .delivery(OrderDelivery.builder()
                        .recipientName(delivery.recipientName())
                        .address(delivery.address())
                        .addressComplement(delivery.addressComplement())
                        .contactPhone(delivery.contactPhone())
                        .build())
                .items(items)
                .statusHistory(List.of())
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private void requireSameAmount(BigDecimal clientAmount, BigDecimal serverAmount, String message) {
        if (clientAmount == null || clientAmount.compareTo(serverAmount) != 0) {
            throw new OrderProductException(message);
        }
    }

    @Override
    public List<Order> findMine(String username) {
        return orderRepository.findByUsername(username);
    }

    @Override
    public Order findOwnedOrder(Long id, String username) {
        return orderRepository.findById(id)
                .filter(order -> order.getUsername().equals(username))
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND));
    }

    @Override
    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND));
    }

    @Override
    public Page<Order> findAll(OrderStatus status, Instant from, Instant to, String username, Pageable pageable) {
        return orderRepository.findAll(status, from, to, username, pageable);
    }

    @Override
    public Order changeStatus(Long id, OrderStatus status, String changedBy) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND));
        OrderStatus current = order.getStatus();
        if (!isTransitionAllowed(current, status)) {
            throw new OrderConflictException("No se permite cambiar el estado de " + current + " a " + status);
        }
        Instant now = Instant.now();
        List<OrderStatusChange> history = new ArrayList<>(order.getStatusHistory());
        history.add(OrderStatusChange.builder()
                .fromStatus(current)
                .toStatus(status)
                .changedBy(changedBy)
                .changedAt(now)
                .build());
        order.setStatus(status);
        order.setStatusHistory(history);
        order.setUpdatedAt(now);
        return orderRepository.save(order);
    }

    private boolean isTransitionAllowed(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case CREADA -> to == OrderStatus.PAGADA || to == OrderStatus.CANCELADA;
            case PAGADA -> to == OrderStatus.DESPACHADA || to == OrderStatus.CANCELADA;
            case DESPACHADA -> to == OrderStatus.ENTREGADA;
            case ENTREGADA, CANCELADA -> false;
        };
    }
}

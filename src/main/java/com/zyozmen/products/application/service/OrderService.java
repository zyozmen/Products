package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.OrderConflictException;
import com.zyozmen.products.domain.exception.OrderProductException;
import com.zyozmen.products.domain.exception.OrderValidationException;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.Order;
import com.zyozmen.products.domain.model.OrderDelivery;
import com.zyozmen.products.domain.model.OrderItem;
import com.zyozmen.products.domain.model.OrderStatus;
import com.zyozmen.products.domain.model.OrderStatusChange;
import com.zyozmen.products.domain.port.in.OrderUseCase;
import com.zyozmen.products.domain.port.out.OrderRepositoryPort;
import com.zyozmen.products.domain.port.in.MarketplaceUseCase;
import com.zyozmen.products.domain.port.out.UserRepositoryPort;
import com.zyozmen.products.domain.port.out.InventarioOrdenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService implements OrderUseCase {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.16");
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal PAYMENT_TOLERANCE = new BigDecimal("0.01");
    private static final String ORDER_NOT_FOUND = "Orden no encontrada";

    private final OrderRepositoryPort orderRepository;
    private final MarketplaceUseCase marketplace;
    private final UserRepositoryPort userRepository;
    private final InventarioOrdenPort inventario;

    // Zona horaria del negocio usada para delimitar los días del reporte.
    @Value("${app.report.timezone:America/Bogota}")
    private String reportTimezone = "America/Bogota";

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
            var product = marketplace.resolverPresentacion(requestedItem.productId());
            BigDecimal unitPrice = product.precioVenta().setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(requestedItem.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            requireSameAmount(requestedItem.unitPrice(), unitPrice, "El precio del producto cambió");
            requireSameAmount(requestedItem.lineTotal(), lineTotal, "El total del producto no coincide");
            items.add(OrderItem.builder()
                    .productId(product.trazabilidad().presentacionId())
                    .name(product.nombre())
                    .quantity(requestedItem.quantity())
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .trazabilidad(product.trazabilidad())
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
    @Transactional
    public Order createPqr(String username, List<PqrItemRequest> requestedItems, OrderDeliveryRequest delivery,
                           String notes) {
        if (username == null || !userRepository.existsByUsername(username)) {
            throw new ResourceNotFoundException("Usuario no encontrado: " + username);
        }
        if (requestedItems == null || requestedItems.isEmpty()) {
            throw new IllegalArgumentException("La orden debe contener al menos un producto");
        }
        List<OrderItem> items = new ArrayList<>();
        for (PqrItemRequest requested : requestedItems) {
            if (requested == null || requested.quantity() <= 0) {
                throw new IllegalArgumentException("La cantidad de cada producto debe ser mayor que cero");
            }
            var product = marketplace.resolverPresentacion(requested.productId());
            items.add(OrderItem.builder()
                    .productId(product.trazabilidad().presentacionId())
                    .name(product.nombre())
                    .quantity(requested.quantity())
                    .unitPrice(ZERO)
                    .lineTotal(ZERO)
                    .trazabilidad(product.trazabilidad())
                    .build());
        }
        OrderDeliveryRequest safeDelivery = delivery == null
                ? new OrderDeliveryRequest("", "", "", "") : delivery;
        inventario.descontar(items);
        Instant now = Instant.now();
        return orderRepository.save(Order.builder()
                .username(username)
                .status(OrderStatus.CREADA)
                .pqr(true)
                .inventoryDeducted(true)
                .notes(notes)
                .subtotal(ZERO)
                .taxAmount(ZERO)
                .total(ZERO)
                .delivery(OrderDelivery.builder()
                        .recipientName(safeDelivery.recipientName())
                        .address(safeDelivery.address())
                        .addressComplement(safeDelivery.addressComplement())
                        .contactPhone(safeDelivery.contactPhone())
                        .build())
                .items(items)
                .statusHistory(List.of())
                .createdAt(now)
                .updatedAt(now)
                .build());
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
    @Transactional
    public Order changeStatus(Long id, OrderStatus status, String changedBy) {
        return changeStatus(id, status, changedBy, StatusChangeDetails.NONE);
    }

    @Override
    @Transactional
    public Order changeStatus(Long id, OrderStatus status, String changedBy, StatusChangeDetails details) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND));
        OrderStatus current = order.getStatus();
        if (!isTransitionAllowed(order, status)) {
            throw new OrderConflictException("No se permite cambiar el estado de " + current + " a " + status);
        }
        StatusChangeDetails safeDetails = details == null ? StatusChangeDetails.NONE : details;
        Instant now = Instant.now();
        switch (status) {
            case PAGADA -> {
                if (!order.isInventoryDeducted()) {
                    inventario.descontar(order.getItems());
                    order.setInventoryDeducted(true);
                }
                order.setCashOnDelivery(Boolean.TRUE.equals(safeDetails.cashOnDelivery()));
                order.setPaidAt(now);
            }
            case EMPACADA -> order.setPackedAt(now);
            case ENTREGADA -> {
                applyDeliveryPayment(order, safeDetails);
                order.setDeliveredAt(now);
            }
            case CANCELADA -> {
                if (order.isInventoryDeducted()) {
                    inventario.restituir(order.getItems());
                    order.setInventoryDeducted(false);
                }
                order.setCancelledAt(now);
            }
            default -> { }
        }
        List<OrderStatusChange> history = new ArrayList<>(order.getStatusHistory());
        history.add(OrderStatusChange.builder()
                .fromStatus(current)
                .toStatus(status)
                .changedBy(changedBy)
                .changedAt(now)
                .reason(status == OrderStatus.CANCELADA ? safeDetails.reason() : null)
                .build());
        order.setStatus(status);
        order.setStatusHistory(history);
        order.setUpdatedAt(now);
        return orderRepository.save(order);
    }

    private void applyDeliveryPayment(Order order, StatusChangeDetails details) {
        if (order.isPqr()) {
            order.setCashAmount(ZERO);
            order.setCardAmount(ZERO);
            return;
        }
        // Una orden sin contra entrega se asume ya pagada: el detalle enviado se ignora.
        BigDecimal total = order.getTotal() == null ? ZERO : order.getTotal();
        if (!order.isCashOnDelivery() || total.signum() <= 0) {
            order.setCashAmount(null);
            order.setCardAmount(null);
            return;
        }
        BigDecimal cash = details.cashAmount();
        BigDecimal card = details.cardAmount();
        if (cash == null || card == null) {
            throw new OrderValidationException(
                    "Para una orden contra entrega se deben informar el efectivo y la tarjeta recibidos");
        }
        if (cash.signum() < 0 || card.signum() < 0) {
            throw new IllegalArgumentException("Los montos de efectivo y tarjeta no pueden ser negativos");
        }
        if (cash.add(card).subtract(total).abs().compareTo(PAYMENT_TOLERANCE) > 0) {
            throw new OrderValidationException("La suma de efectivo y tarjeta debe ser igual al total de la orden");
        }
        order.setCashAmount(cash.setScale(2, RoundingMode.HALF_UP));
        order.setCardAmount(card.setScale(2, RoundingMode.HALF_UP));
    }

    private boolean isTransitionAllowed(Order order, OrderStatus to) {
        return switch (order.getStatus()) {
            case CREADA -> to == OrderStatus.CANCELADA
                    || to == (order.isPqr() ? OrderStatus.EMPACADA : OrderStatus.PAGADA);
            case PAGADA -> to == OrderStatus.EMPACADA || to == OrderStatus.CANCELADA;
            case EMPACADA -> to == OrderStatus.ENTREGADA || to == OrderStatus.CANCELADA;
            case ENTREGADA, CANCELADA -> false;
        };
    }

    @Override
    public OrderReport report(LocalDate date, ReportPeriod period) {
        ZoneId zone = ZoneId.of(reportTimezone);
        LocalDate from = switch (period) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
        };
        LocalDate to = switch (period) {
            case DAY -> from.plusDays(1);
            case WEEK -> from.plusWeeks(1);
            case MONTH -> from.plusMonths(1);
        };
        List<Order> orders = orderRepository.findAll(null, from.atStartOfDay(zone).toInstant(),
                to.atStartOfDay(zone).toInstant(), null, null).getContent();

        long paidCount = 0;
        BigDecimal paidTotal = ZERO;
        BigDecimal cash = ZERO;
        BigDecimal card = ZERO;
        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.ENTREGADA && !order.isPqr()) {
                paidCount++;
                paidTotal = paidTotal.add(order.getTotal() == null ? ZERO : order.getTotal());
                cash = cash.add(order.getCashAmount() == null ? ZERO : order.getCashAmount());
                card = card.add(order.getCardAmount() == null ? ZERO : order.getCardAmount());
            }
        }
        BigDecimal undetailed = paidTotal.subtract(cash).subtract(card);
        return new OrderReport(from, to, orders.size(),
                new PaidSummary(paidCount, paidTotal, cash, card, undetailed),
                new CountSummary(countByStatus(orders, OrderStatus.CANCELADA)),
                new CountSummary(countByStatus(orders, OrderStatus.PAGADA)),
                new CountSummary(countByStatus(orders, OrderStatus.EMPACADA)),
                new CountSummary(orders.stream().filter(Order::isPqr).count()));
    }

    private long countByStatus(List<Order> orders, OrderStatus status) {
        return orders.stream().filter(order -> order.getStatus() == status).count();
    }
}

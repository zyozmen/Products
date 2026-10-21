package com.zyozmen.products.adapter.in.web.dto;

import com.zyozmen.products.domain.model.Order;
import com.zyozmen.products.domain.model.OrderDelivery;
import com.zyozmen.products.domain.model.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponseDTO(
        Long id,
        String username,
        String status,
        boolean pqr,
        boolean cashOnDelivery,
        BigDecimal cashAmount,
        BigDecimal cardAmount,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        Instant paidAt,
        Instant packedAt,
        Instant deliveredAt,
        Instant cancelledAt,
        List<ItemResponse> items,
        DeliveryResponse delivery,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal total) {

    public static OrderResponseDTO from(Order order) {
        return new OrderResponseDTO(order.getId(), order.getUsername(), order.getStatus().name(),
                order.isPqr(), order.isCashOnDelivery(), order.getCashAmount(), order.getCardAmount(),
                order.getNotes(), order.getCreatedAt(), order.getUpdatedAt(), order.getPaidAt(),
                order.getPackedAt(), order.getDeliveredAt(), order.getCancelledAt(),
                order.getItems().stream().map(OrderResponseDTO::toItem).toList(),
                toDelivery(order.getDelivery()), order.getSubtotal(), order.getTaxAmount(), order.getTotal());
    }

    private static ItemResponse toItem(OrderItem item) {
        return new ItemResponse(item.getProductId(), item.getName(), item.getQuantity(),
                item.getUnitPrice(), item.getLineTotal());
    }

    private static DeliveryResponse toDelivery(OrderDelivery delivery) {
        return new DeliveryResponse(delivery.getRecipientName(), delivery.getAddress(),
                delivery.getAddressComplement(), delivery.getContactPhone());
    }

    public record ItemResponse(String productId, String name, int quantity, BigDecimal unitPrice,
                               BigDecimal lineTotal) { }
    public record DeliveryResponse(String recipientName, String address, String addressComplement,
                                   String contactPhone) { }
}

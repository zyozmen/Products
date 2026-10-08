package com.zyozmen.products.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private Long id;
    private String username;
    private OrderStatus status;
    private boolean pqr;
    private boolean cashOnDelivery;
    private BigDecimal cashAmount;
    private BigDecimal cardAmount;
    private String notes;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal total;
    private OrderDelivery delivery;
    private List<OrderItem> items;
    private List<OrderStatusChange> statusHistory;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant paidAt;
    private Instant packedAt;
    private Instant deliveredAt;
    private Instant cancelledAt;
    private Long version;
}

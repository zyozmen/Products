package com.zyozmen.products.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChange {
    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    private String changedBy;
    private Instant changedAt;
}

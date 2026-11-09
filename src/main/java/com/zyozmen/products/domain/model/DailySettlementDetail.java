package com.zyozmen.products.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DailySettlementDetail(
        String id, String movementType, String providerId, Long orderId, Integer itemIndex,
        String lotId, String containerId, String strainId, String presentationId,
        Integer quantity, BigDecimal contentPerUnit, String baseUnit, BigDecimal baseUnitCost,
        BigDecimal unitCost, BigDecimal amount, Instant timestamp, LocalDate date,
        String originalMovementId, String correlationId, String paymentType, String observations,
        List<PaymentAllocation> allocations,
        String providerName, String lotCode, String containerCode, String presentationName,
        String productName) {

    public DailySettlementDetail {
        allocations = allocations == null ? List.of() : List.copyOf(allocations);
    }

    public record PaymentAllocation(String transactionId, String lotId, BigDecimal amount, String lotCode) { }
}

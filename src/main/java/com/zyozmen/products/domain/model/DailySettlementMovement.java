package com.zyozmen.products.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record DailySettlementMovement(
        Long orderId, int itemIndex, String movementType, String providerId,
        String lotId, String containerId, String strainId, String presentationId,
        int quantity, BigDecimal contentPerUnit, String baseUnit, BigDecimal baseUnitCost,
        BigDecimal unitCost, BigDecimal amount, Instant timestamp, LocalDate date) { }

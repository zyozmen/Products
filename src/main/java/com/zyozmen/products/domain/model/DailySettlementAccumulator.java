package com.zyozmen.products.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record DailySettlementAccumulator(
        String providerId, LocalDate date, long totalSalesCount, long totalPqrCount,
        BigDecimal salesRecognizedAmount, BigDecimal pqrRecognizedAmount,
        BigDecimal reversedAmount, BigDecimal linkedPaymentsAmount) {

    public DailySettlementAccumulator {
        Objects.requireNonNull(providerId, "El acumulado debe tener proveedor");
        Objects.requireNonNull(date, "El acumulado debe tener fecha");
        for (BigDecimal amount : new BigDecimal[]{
                salesRecognizedAmount, pqrRecognizedAmount, reversedAmount, linkedPaymentsAmount}) {
            if (amount == null || amount.signum() < 0) {
                throw new IllegalStateException("El acumulado contiene importes ausentes o negativos");
            }
        }
        if (totalSalesCount < 0 || totalPqrCount < 0
                || reversedAmount.compareTo(pqrRecognizedAmount) > 0) {
            throw new IllegalStateException("El acumulado contiene cantidades o reversiones inconsistentes");
        }
    }
}

package com.zyozmen.products.domain.port.in;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public interface DailySettlementUseCase {
    DailySummary summary(LocalDate date, String providerId, int page, int size);

    record DailySummary(LocalDate date, String timezone, boolean activationPending,
                        Instant activatedAt, Page<ProviderSummary> providers) { }

    record ProviderSummary(String providerId, String providerName, LocalDate date,
                           long totalSalesCount, long totalPqrCount,
                           BigDecimal salesRecognizedAmount, BigDecimal pqrRecognizedAmount,
                           BigDecimal reversedAmount, BigDecimal grossAccumulatedAmount,
                           BigDecimal accumulatedAmount, BigDecimal linkedPaymentsAmount,
                           BigDecimal pendingDailyAmount, BigDecimal providerOutstandingBalance,
                           BigDecimal suggestedPayableAmount, BigDecimal overpaidAmount) { }
}

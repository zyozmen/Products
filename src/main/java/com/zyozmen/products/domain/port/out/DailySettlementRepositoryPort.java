package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.DailySettlementAccumulator;
import com.zyozmen.products.domain.model.DailySettlementMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

public interface DailySettlementRepositoryPort {
    Page<ProviderAccumulator> findByDate(LocalDate date, String providerId, Pageable pageable);

    Optional<Instant> activationAt();

    void recognizeSales(List<DailySettlementMovement> movements);

    void recognizePqrs(List<DailySettlementMovement> movements);

    void reversePqr(Long orderId, Instant cancelledAt);

    record ProviderAccumulator(DailySettlementAccumulator accumulator, String providerName,
                               BigDecimal providerOutstandingBalance) { }
}

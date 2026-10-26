package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.port.in.DailySettlementUseCase;
import com.zyozmen.products.domain.port.out.DailySettlementRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

@Service
public class DailySettlementService implements DailySettlementUseCase {
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final DailySettlementRepositoryPort repository;
    private final Clock clock;

    @Autowired
    public DailySettlementService(DailySettlementRepositoryPort repository) {
        this(repository, Clock.systemUTC());
    }

    DailySettlementService(DailySettlementRepositoryPort repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public DailySummary summary(LocalDate date, String providerId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page debe ser >= 0 y size entre 1 y 100");
        }
        if (providerId != null && !providerId.matches("[a-fA-F0-9]{24}")) {
            throw new IllegalArgumentException("providerId debe ser un ObjectId valido");
        }
        LocalDate day = date == null ? LocalDate.now(clock.withZone(BUSINESS_ZONE)) : date;
        var activation = repository.activationAt();
        var providers = repository.findByDate(day,
                providerId == null ? null : providerId.toLowerCase(Locale.ROOT),
                PageRequest.of(page, size, Sort.by("providerId"))).map(this::toSummary);
        return new DailySummary(day, BUSINESS_ZONE.getId(), activation.isEmpty(),
                activation.orElse(null), providers);
    }

    private ProviderSummary toSummary(DailySettlementRepositoryPort.ProviderAccumulator source) {
        var a = source.accumulator();
        var balance = source.providerOutstandingBalance();
        if (source.providerName() == null || balance == null || balance.signum() < 0) {
            throw new IllegalStateException("El proveedor del acumulado no tiene nombre o saldo valido");
        }
        var gross = a.salesRecognizedAmount().add(a.pqrRecognizedAmount());
        var accumulated = gross.subtract(a.reversedAmount());
        var pending = accumulated.subtract(a.linkedPaymentsAmount()).max(ZERO);
        return new ProviderSummary(a.providerId(), source.providerName(), a.date(),
                a.totalSalesCount(), a.totalPqrCount(), money(a.salesRecognizedAmount()),
                money(a.pqrRecognizedAmount()), money(a.reversedAmount()), money(gross),
                money(accumulated), money(a.linkedPaymentsAmount()), money(pending), money(balance),
                money(pending.min(balance)), money(a.linkedPaymentsAmount().subtract(accumulated).max(ZERO)));
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }
}

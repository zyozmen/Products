package com.zyozmen.products.adapter.in.web.dto;

import com.zyozmen.products.domain.port.in.OrderUseCase;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderReportResponseDTO(
        LocalDate from,
        LocalDate to,
        long totalOrders,
        Paid paid,
        Count cancelled,
        Count toPack,
        Count toDeliver,
        Count pqr) {

    public static OrderReportResponseDTO from(OrderUseCase.OrderReport report) {
        OrderUseCase.PaidSummary paid = report.paid();
        return new OrderReportResponseDTO(report.from(), report.to(), report.totalOrders(),
                new Paid(paid.count(), paid.total(), paid.cash(), paid.card(), paid.undetailed()),
                new Count(report.cancelled().count()), new Count(report.toPack().count()),
                new Count(report.toDeliver().count()), new Count(report.pqr().count()));
    }

    public record Paid(long count, BigDecimal total, BigDecimal cash, BigDecimal card, BigDecimal undetailed) { }
    public record Count(long count) { }
}

package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.domain.port.in.DailySettlementUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Liquidacion diaria", description = "Resumen diario de costos reconocidos al proveedor")
public class DailySettlementController {
    private final DailySettlementUseCase useCase;

    public record DailySummaryResponse(LocalDate date, String timezone, boolean activationPending,
                                       Instant activatedAt, List<DailySettlementUseCase.ProviderSummary> content,
                                       int page, int size, long totalElements, int totalPages) { }

    @GetMapping("/api/v1/settlements/daily-summary")
    @Operation(summary = "Consultar el resumen diario paginado por proveedor (ADMIN)")
    public DailySummaryResponse summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String providerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var summary = useCase.summary(date, providerId, page, size);
        var providers = summary.providers();
        return new DailySummaryResponse(summary.date(), summary.timezone(), summary.activationPending(),
                summary.activatedAt(), providers.getContent(), providers.getNumber(), providers.getSize(),
                providers.getTotalElements(), providers.getTotalPages());
    }
}

package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.domain.port.in.DailySettlementUseCase;
import com.zyozmen.products.domain.model.DailySettlementDetail;
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
import java.util.Locale;

@RestController
@RequiredArgsConstructor
@Tag(name = "Liquidacion diaria", description = "Resumen diario de costos reconocidos al proveedor")
public class DailySettlementController {
    private final DailySettlementUseCase useCase;

    public record DailySummaryResponse(LocalDate date, String timezone, boolean activationPending,
                                       Instant activatedAt, List<DailySettlementUseCase.ProviderSummary> content,
                                       int page, int size, long totalElements, int totalPages) { }

    public record DailyDetailResponse(String providerId, LocalDate date,
                                      List<DailySettlementDetail> content, int page, int size,
                                      long totalElements, int totalPages) { }

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

    @GetMapping("/api/v1/settlements/daily-detail")
    @Operation(summary = "Consultar el detalle auditable paginado de un proveedor y jornada (ADMIN)")
    public DailyDetailResponse detail(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String providerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var details = useCase.detail(date, providerId, page, size);
        return new DailyDetailResponse(providerId.toLowerCase(Locale.ROOT), date,
                details.getContent(), details.getNumber(), details.getSize(),
                details.getTotalElements(), details.getTotalPages());
    }
}

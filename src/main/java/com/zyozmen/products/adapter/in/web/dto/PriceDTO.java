package com.zyozmen.products.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estructura de precio del producto")
public class PriceDTO {

    @Schema(description = "Precio actual", example = "199.99")
    private BigDecimal current;

    @Schema(description = "Precio original", example = "249.99")
    private BigDecimal original;

    @Schema(description = "Moneda", example = "USD")
    private String currency;

    @JsonProperty("discount_percentage")
    @Schema(description = "Porcentaje de descuento", example = "20")
    private Integer discountPercentage;

    @JsonProperty("tax_inclusive")
    @Schema(description = "Si el precio incluye impuestos", example = "true")
    private Boolean taxInclusive;
}

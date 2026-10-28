package com.zyozmen.products.domain.model;

import java.util.Locale;

public enum UnitMeasurement {
    GRAMOS, MILILITROS, UNIDADES;

    public static UnitMeasurement desde(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("La unidad base es obligatoria");
        }
        return switch (valor.strip().toUpperCase(Locale.ROOT)) {
            case "G", "GR", "GRAMO", "GRAMOS" -> GRAMOS;
            case "ML", "MILILITRO", "MILILITROS" -> MILILITROS;
            case "UNIDAD", "UNIDADES" -> UNIDADES;
            default -> throw new IllegalArgumentException("Unidad no soportada: use GRAMOS, MILILITROS o UNIDADES");
        };
    }
}

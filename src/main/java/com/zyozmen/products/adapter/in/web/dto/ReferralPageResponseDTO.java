package com.zyozmen.products.adapter.in.web.dto;

import java.time.Instant;
import java.util.List;

public record ReferralPageResponseDTO(List<Item> content, int page, int size,
                                      long totalElements, long totalPages) {
    public record Item(String username, String nombre, String apellido, Instant createdAt) {
    }
}

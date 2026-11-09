package com.zyozmen.products.domain.model;

import java.time.Instant;
import java.util.List;

public record ReferralPage(List<Item> content, int page, int size, long totalElements) {
    public record Item(String username, String nombre, String apellido, Instant createdAt) {
    }
}

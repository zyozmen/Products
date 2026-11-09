package com.zyozmen.products.domain.model;

import java.time.Instant;

public record Referral(String idReferente, String idReferido, Instant createdAt) {
}

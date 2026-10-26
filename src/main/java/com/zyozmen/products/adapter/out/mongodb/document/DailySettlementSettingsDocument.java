package com.zyozmen.products.adapter.out.mongodb.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "daily_settlement_settings")
public record DailySettlementSettingsDocument(@Id String id, Instant activatedAt) {
    public static final String ID = "daily-settlement";
}

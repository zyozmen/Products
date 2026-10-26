package com.zyozmen.products.adapter.out.mongodb.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "provider_daily_movements")
public record DailySettlementMovementDocument(
        @Id String id, Long orderId, int itemIndex, String movementType,
        @Field(targetType = FieldType.OBJECT_ID) String providerId,
        @Field(targetType = FieldType.OBJECT_ID) String lotId,
        @Field(targetType = FieldType.OBJECT_ID) String containerId,
        String strainId,
        @Field(targetType = FieldType.OBJECT_ID) String presentationId,
        int quantity,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal contentPerUnit,
        String baseUnit,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal baseUnitCost,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal unitCost,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal amount,
        Instant timestamp, String date,
        @Field(targetType = FieldType.OBJECT_ID) String originalMovementId) { }

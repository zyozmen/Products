package com.zyozmen.products.adapter.out.mongodb.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

@Document(collection = "ProviderAccumulators")
public record ProviderAccumulatorsDocument(
        @Id String id,
        @Field(targetType = FieldType.OBJECT_ID) String providerId,
        String date,
        long totalSalesCount, long totalPqrCount,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal salesRecognizedAmount,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal pqrRecognizedAmount,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal reversedAmount,
        @Field(targetType = FieldType.DECIMAL128) BigDecimal linkedPaymentsAmount) { }

package com.zyozmen.products.adapter.out.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document(collection = "orders")
// Embedding items and status history makes creating and updating an order a single atomic document write.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMongoDocument {
    @Id
    private String id;
    @Field("username")
    private String username;
    @Field("status")
    private String status;
    @Field("subtotal")
    private BigDecimal subtotal;
    @Field("tax_amount")
    private BigDecimal taxAmount;
    @Field("total")
    private BigDecimal total;
    @Field("delivery")
    private DeliveryDocument delivery;
    @Field("items")
    private List<ItemDocument> items;
    @Field("status_history")
    private List<StatusChangeDocument> statusHistory;
    @Field("created_at")
    private Instant createdAt;
    @Field("updated_at")
    private Instant updatedAt;
    @Version
    private Long version;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDocument {
        @Field("product_id")
        private String productId;
        @Field("name")
        private String name;
        @Field("quantity")
        private int quantity;
        @Field("unit_price")
        private BigDecimal unitPrice;
        @Field("line_total")
        private BigDecimal lineTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryDocument {
        @Field("recipient_name")
        private String recipientName;
        @Field("address")
        private String address;
        @Field("address_complement")
        private String addressComplement;
        @Field("contact_phone")
        private String contactPhone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusChangeDocument {
        @Field("from_status")
        private String fromStatus;
        @Field("to_status")
        private String toStatus;
        @Field("changed_by")
        private String changedBy;
        @Field("changed_at")
        private Instant changedAt;
    }
}

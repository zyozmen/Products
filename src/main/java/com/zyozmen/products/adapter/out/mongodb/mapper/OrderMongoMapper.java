package com.zyozmen.products.adapter.out.mongodb.mapper;

import com.zyozmen.products.adapter.out.mongodb.document.OrderMongoDocument;
import com.zyozmen.products.domain.model.Order;
import com.zyozmen.products.domain.model.OrderDelivery;
import com.zyozmen.products.domain.model.OrderItem;
import com.zyozmen.products.domain.model.OrderStatus;
import com.zyozmen.products.domain.model.OrderStatusChange;
import org.springframework.stereotype.Component;

@Component
public class OrderMongoMapper {
    public Order toDomain(OrderMongoDocument document) {
        return Order.builder()
                .id(document.getId() == null ? null : Long.valueOf(document.getId()))
                .username(document.getUsername())
                .status(OrderStatus.valueOf(document.getStatus()))
                .subtotal(document.getSubtotal())
                .taxAmount(document.getTaxAmount())
                .total(document.getTotal())
                .delivery(OrderDelivery.builder()
                        .recipientName(document.getDelivery().getRecipientName())
                        .address(document.getDelivery().getAddress())
                        .addressComplement(document.getDelivery().getAddressComplement())
                        .contactPhone(document.getDelivery().getContactPhone())
                        .build())
                .items(document.getItems().stream().map(item -> OrderItem.builder()
                        .productId(item.getProductId())
                        .name(item.getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getLineTotal())
                        .build()).toList())
                .statusHistory(document.getStatusHistory().stream().map(change -> OrderStatusChange.builder()
                        .fromStatus(OrderStatus.valueOf(change.getFromStatus()))
                        .toStatus(OrderStatus.valueOf(change.getToStatus()))
                        .changedBy(change.getChangedBy())
                        .changedAt(change.getChangedAt())
                        .build()).toList())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .version(document.getVersion())
                .build();
    }

    public OrderMongoDocument toDocument(Order order) {
        return OrderMongoDocument.builder()
                .id(order.getId() == null ? null : order.getId().toString())
                .username(order.getUsername())
                .status(order.getStatus().name())
                .subtotal(order.getSubtotal())
                .taxAmount(order.getTaxAmount())
                .total(order.getTotal())
                .delivery(OrderMongoDocument.DeliveryDocument.builder()
                        .recipientName(order.getDelivery().getRecipientName())
                        .address(order.getDelivery().getAddress())
                        .addressComplement(order.getDelivery().getAddressComplement())
                        .contactPhone(order.getDelivery().getContactPhone())
                        .build())
                .items(order.getItems().stream().map(item -> OrderMongoDocument.ItemDocument.builder()
                        .productId(item.getProductId())
                        .name(item.getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getLineTotal())
                        .build()).toList())
                .statusHistory(order.getStatusHistory().stream().map(change ->
                        OrderMongoDocument.StatusChangeDocument.builder()
                                .fromStatus(change.getFromStatus().name())
                                .toStatus(change.getToStatus().name())
                                .changedBy(change.getChangedBy())
                                .changedAt(change.getChangedAt())
                                .build()).toList())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .version(order.getVersion())
                .build();
    }
}

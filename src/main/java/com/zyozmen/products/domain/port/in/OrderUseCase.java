package com.zyozmen.products.domain.port.in;

import com.zyozmen.products.domain.model.Order;
import com.zyozmen.products.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface OrderUseCase {
    Order create(String username, List<OrderItemRequest> items, OrderDeliveryRequest delivery,
                 OrderAmountsRequest amounts);
    List<Order> findMine(String username);
    Order findOwnedOrder(Long id, String username);
    Order findById(Long id);
    Page<Order> findAll(OrderStatus status, Instant from, Instant to, String username, Pageable pageable);
    Order changeStatus(Long id, OrderStatus status, String changedBy);

    record OrderItemRequest(String productId, int quantity, java.math.BigDecimal unitPrice,
                            java.math.BigDecimal lineTotal) { }
    record OrderDeliveryRequest(String recipientName, String address, String addressComplement,
                                String contactPhone) { }
    record OrderAmountsRequest(java.math.BigDecimal subtotal, java.math.BigDecimal taxAmount,
                               java.math.BigDecimal total) { }
}

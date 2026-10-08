package com.zyozmen.products.domain.port.in;

import com.zyozmen.products.domain.model.Order;
import com.zyozmen.products.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface OrderUseCase {
    Order create(String username, List<OrderItemRequest> items, OrderDeliveryRequest delivery,
                 OrderAmountsRequest amounts);
    Order createPqr(String username, List<PqrItemRequest> items, OrderDeliveryRequest delivery, String notes);
    List<Order> findMine(String username);
    Order findOwnedOrder(Long id, String username);
    Order findById(Long id);
    Page<Order> findAll(OrderStatus status, Instant from, Instant to, String username, Pageable pageable);
    Order changeStatus(Long id, OrderStatus status, String changedBy, StatusChangeDetails details);
    OrderReport report(LocalDate date, ReportPeriod period);

    default Order changeStatus(Long id, OrderStatus status, String changedBy) {
        return changeStatus(id, status, changedBy, StatusChangeDetails.NONE);
    }

    enum ReportPeriod { DAY, WEEK, MONTH }

    record OrderItemRequest(String productId, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) { }
    record PqrItemRequest(String productId, String name, int quantity) { }
    record OrderDeliveryRequest(String recipientName, String address, String addressComplement,
                                String contactPhone) { }
    record OrderAmountsRequest(BigDecimal subtotal, BigDecimal taxAmount, BigDecimal total) { }

    record StatusChangeDetails(Boolean cashOnDelivery, BigDecimal cashAmount, BigDecimal cardAmount,
                               String reason) {
        public static final StatusChangeDetails NONE = new StatusChangeDetails(null, null, null, null);
    }

    /** {@code to} es exclusivo: el rango consultado es [from, to). */
    record OrderReport(LocalDate from, LocalDate to, long totalOrders, PaidSummary paid,
                       CountSummary cancelled, CountSummary toPack, CountSummary toDeliver,
                       CountSummary pqr) { }
    record PaidSummary(long count, BigDecimal total, BigDecimal cash, BigDecimal card, BigDecimal undetailed) { }
    record CountSummary(long count) { }
}

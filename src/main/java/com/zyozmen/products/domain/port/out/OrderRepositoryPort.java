package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.Order;
import com.zyozmen.products.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepositoryPort {
    Order save(Order order);
    Optional<Order> findById(Long id);
    List<Order> findByUsername(String username);
    Page<Order> findAll(OrderStatus status, Instant from, Instant to, String username, Pageable pageable);
}

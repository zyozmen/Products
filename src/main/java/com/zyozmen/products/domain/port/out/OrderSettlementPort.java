package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.Order;

public interface OrderSettlementPort {
    void recognizeOrderMovement(Order order);
}

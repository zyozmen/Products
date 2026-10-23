package com.zyozmen.products.domain.port.out;

import com.zyozmen.products.domain.model.OrderItem;
import java.util.List;

public interface InventarioOrdenPort {
    void descontar(List<OrderItem> items);
    void restituir(List<OrderItem> items);
}

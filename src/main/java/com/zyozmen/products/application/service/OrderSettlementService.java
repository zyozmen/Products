package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.OrderProductException;
import com.zyozmen.products.domain.model.DailySettlementMovement;
import com.zyozmen.products.domain.model.Order;
import com.zyozmen.products.domain.model.OrderStatus;
import com.zyozmen.products.domain.port.out.DailySettlementRepositoryPort;
import com.zyozmen.products.domain.port.out.OrderSettlementPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class OrderSettlementService implements OrderSettlementPort {
    private final DailySettlementRepositoryPort repository;

    @Override
    public void recognizeOrderMovement(Order order) {
        if (order.isPqr() && order.getStatus() == OrderStatus.CANCELADA) {
            if (order.getId() == null || order.getCancelledAt() == null) {
                throw new OrderProductException("La PQR no tiene identidad o fecha de cancelacion");
            }
            repository.reversePqr(order.getId(), order.getCancelledAt());
            return;
        }
        var recognitionStatus = order.isPqr() ? OrderStatus.CREADA
                : order.isCashOnDelivery() ? OrderStatus.ENTREGADA : OrderStatus.PAGADA;
        if (order.getStatus() != recognitionStatus) {
            return;
        }
        var recognizedAt = order.isPqr() ? order.getCreatedAt()
                : order.isCashOnDelivery() ? order.getDeliveredAt() : order.getPaidAt();
        var activation = repository.activationAt()
                .orElseThrow(() -> new IllegalStateException("La liquidacion diaria no tiene fecha de inicio"));
        if (order.getCreatedAt() == null) {
            throw new OrderProductException("La orden no tiene fecha de creacion para liquidacion");
        }
        if (order.getCreatedAt().isBefore(activation)) {
            return;
        }
        if (order.getId() == null || recognizedAt == null || recognizedAt.isBefore(order.getCreatedAt())
                || order.getItems() == null || order.getItems().isEmpty()) {
            throw new OrderProductException("La orden no tiene datos validos para reconocer el costo");
        }
        var movements = new ArrayList<DailySettlementMovement>();
        for (int index = 0; index < order.getItems().size(); index++) {
            var item = order.getItems().get(index);
            var t = item == null ? null : item.getTrazabilidad();
            if (t == null || !objectId(t.proveedorId()) || !objectId(t.loteId())
                    || !objectId(t.contenedorId()) || !objectId(t.presentacionId())
                    || t.cepaId() == null || t.cepaId().isBlank()
                    || !t.presentacionId().equals(item.getProductId()) || item.getQuantity() <= 0
                    || t.unidadBase() == null || t.unidadBase().isBlank()
                    || t.contenidoUnidad() == null || t.contenidoUnidad().signum() <= 0
                    || t.costoBasePorUnidad() == null || t.costoBasePorUnidad().signum() < 0
                    || t.costoPresentacion() == null || t.costoPresentacion().signum() < 0) {
                throw new OrderProductException("El item no tiene un snapshot valido para liquidacion");
            }
            BigDecimal expected = t.contenidoUnidad().multiply(t.costoBasePorUnidad())
                    .setScale(2, RoundingMode.HALF_UP);
            if (expected.compareTo(t.costoPresentacion()) != 0) {
                throw new OrderProductException("El costo del snapshot no coincide con contenido y costo base");
            }
            var unitCost = t.costoPresentacion().setScale(2, RoundingMode.UNNECESSARY);
            movements.add(new DailySettlementMovement(order.getId(), index, order.isPqr() ? "PQR" : "SALE", t.proveedorId(),
                    t.loteId(), t.contenedorId(), t.cepaId(), t.presentacionId(), item.getQuantity(),
                    t.contenidoUnidad(), t.unidadBase(), t.costoBasePorUnidad(), unitCost,
                    unitCost.multiply(BigDecimal.valueOf(item.getQuantity())), recognizedAt,
                    recognizedAt.atZone(DailySettlementService.BUSINESS_ZONE).toLocalDate()));
        }
        if (order.isPqr()) {
            repository.recognizePqrs(movements);
        } else {
            repository.recognizeSales(movements);
        }
    }

    private boolean objectId(String value) {
        return value != null && value.matches("[a-fA-F0-9]{24}");
    }
}

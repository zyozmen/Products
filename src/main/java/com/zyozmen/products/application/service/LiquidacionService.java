package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.model.Provider;
import com.zyozmen.products.domain.port.in.LiquidacionUseCase;
import com.zyozmen.products.domain.port.out.LoteRepositoryPort;
import com.zyozmen.products.domain.port.out.ProveedorRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class LiquidacionService implements LiquidacionUseCase {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final ProveedorRepositoryPort proveedorRepository;
    private final LoteRepositoryPort loteRepository;

    @Override
    public EvaluacionLiquidacion evaluarEvento(EventoLiquidacionCommand command) {
        Provider proveedor = proveedorRepository.findById(command.proveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
        Lote lote = loteRepository.findLoteById(command.loteId())
                .filter(value -> proveedor.getId().equals(value.getProveedorId()))
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado para el proveedor"));

        // Tope de pago: sin saldo pendiente no se liquida, pero el stock siempre se descuenta.
        if (lote.getSaldoPendienteLote().signum() == 0) {
            return sinLiquidacion("El lote no tiene saldo pendiente");
        }
        if (proveedor.getSaldoPendiente().signum() == 0) {
            return sinLiquidacion("El proveedor no tiene saldo pendiente");
        }

        BigDecimal tope = lote.getSaldoPendienteLote().min(proveedor.getSaldoPendiente());
        BigDecimal liquidable = command.montoLiquidable().setScale(2, RoundingMode.HALF_UP).min(tope);
        return new EvaluacionLiquidacion(true, true, liquidable, "Acumula a la liquidaciÃ³n diaria");
    }

    private EvaluacionLiquidacion sinLiquidacion(String motivo) {
        return new EvaluacionLiquidacion(false, true, ZERO, motivo);
    }
}

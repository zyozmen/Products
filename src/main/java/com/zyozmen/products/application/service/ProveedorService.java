package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.BusinessRuleException;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.EstadoPagoLote;
import com.zyozmen.products.domain.model.EstadoProveedor;
import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.model.Presentacion;
import com.zyozmen.products.domain.model.Proveedor;
import com.zyozmen.products.domain.model.TipoTransaccionProveedor;
import com.zyozmen.products.domain.model.TransaccionProveedor;
import com.zyozmen.products.domain.port.in.ProveedorUseCase;
import com.zyozmen.products.domain.port.out.LoteRepositoryPort;
import com.zyozmen.products.domain.port.out.ProveedorRepositoryPort;
import com.zyozmen.products.domain.port.out.TransaccionProveedorRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProveedorService implements ProveedorUseCase {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final String PROVEEDOR_NO_ENCONTRADO = "Proveedor no encontrado";

    private final ProveedorRepositoryPort proveedorRepository;
    private final LoteRepositoryPort loteRepository;
    private final TransaccionProveedorRepositoryPort transaccionRepository;

    @Override
    @Transactional
    public Proveedor crear(CrearProveedorCommand command) {
        if (proveedorRepository.existsByNit(command.nit())) {
            throw new BusinessRuleException("Ya existe un proveedor con el NIT " + command.nit());
        }
        Instant now = Instant.now();
        return proveedorRepository.save(Proveedor.builder()
                .nombre(command.nombre())
                .nit(command.nit())
                .telefono(command.telefono())
                .email(command.email())
                .totalDeuda(ZERO)
                .totalPagado(ZERO)
                .saldoPendiente(ZERO)
                .estado(EstadoProveedor.ACTIVO)
                .fechaCreacion(now)
                .ultimaActualizacion(now)
                .build());
    }

    @Override
    public Proveedor obtener(String id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PROVEEDOR_NO_ENCONTRADO));
    }

    @Override
    @Transactional
    public Lote recibirLote(RecibirLoteCommand command) {
        Proveedor proveedor = obtener(command.proveedorId());
        if (proveedor.getEstado() != EstadoProveedor.ACTIVO) {
            throw new BusinessRuleException("El proveedor no estÃ¡ activo");
        }
        if (loteRepository.existsByCodigoLote(command.codigoLote())) {
            throw new BusinessRuleException("Ya existe un lote con el cÃ³digo " + command.codigoLote());
        }

        List<Presentacion> presentaciones = new ArrayList<>();
        BigDecimal costoTotal = ZERO;
        for (PresentacionCommand item : command.presentaciones()) {
            BigDecimal costoUnitario = money(item.costoUnitario());
            BigDecimal subtotal = costoUnitario.multiply(BigDecimal.valueOf(item.cantidadUnidades()))
                    .setScale(2, RoundingMode.HALF_UP);
            presentaciones.add(Presentacion.builder()
                    .presentacionId(item.presentacionId())
                    .cantidadUnidades(item.cantidadUnidades())
                    .costoUnitario(costoUnitario)
                    .subtotal(subtotal)
                    .build());
            costoTotal = costoTotal.add(subtotal);
        }

        BigDecimal abono = command.abonoInicial() == null ? ZERO : money(command.abonoInicial());
        if (abono.signum() < 0) {
            throw new BusinessRuleException("El abono inicial no puede ser negativo");
        }
        if (abono.compareTo(costoTotal) > 0) {
            throw new BusinessRuleException("El abono inicial no puede superar el costo total del lote");
        }

        Instant now = Instant.now();
        BigDecimal saldoLote = costoTotal.subtract(abono);
        Lote lote = loteRepository.save(Lote.builder()
                .proveedorId(proveedor.getId())
                .codigoLote(command.codigoLote())
                .costoTotalLote(costoTotal)
                .montoPagadoLote(abono)
                .saldoPendienteLote(saldoLote)
                .estadoPago(estadoPago(abono, saldoLote))
                .presentaciones(presentaciones)
                .fechaIngreso(now)
                .build());

        registrarTransaccion(proveedor.getId(), lote.getId(), TipoTransaccionProveedor.DEUDA_LOTE, costoTotal,
                "Deuda generada por el lote " + lote.getCodigoLote(), now);
        proveedor.setTotalDeuda(proveedor.getTotalDeuda().add(costoTotal));
        if (abono.signum() > 0) {
            registrarTransaccion(proveedor.getId(), lote.getId(), TipoTransaccionProveedor.ABONO_INICIAL, abono,
                    "Abono inicial al recibir el lote " + lote.getCodigoLote(), now);
            proveedor.setTotalPagado(proveedor.getTotalPagado().add(abono));
        }
        actualizarSaldo(proveedor, now);
        proveedorRepository.save(proveedor);
        return lote;
    }

    @Override
    @Transactional
    public Proveedor registrarPago(RegistrarPagoCommand command) {
        TipoTransaccionProveedor tipo = command.tipo() == null ? TipoTransaccionProveedor.ABONO : command.tipo();
        if (tipo == TipoTransaccionProveedor.DEUDA_LOTE || tipo == TipoTransaccionProveedor.ABONO_INICIAL) {
            throw new BusinessRuleException("El tipo " + tipo + " no es vÃ¡lido para un pago posterior");
        }
        BigDecimal monto = money(command.monto());
        if (monto.signum() <= 0) {
            throw new BusinessRuleException("El monto del pago debe ser mayor que cero");
        }

        Proveedor proveedor = obtener(command.proveedorId());
        if (monto.compareTo(proveedor.getSaldoPendiente()) > 0) {
            throw new BusinessRuleException("El pago no puede superar el saldo pendiente del proveedor");
        }

        Instant now = Instant.now();
        if (command.loteId() != null) {
            Lote lote = loteRepository.findLoteById(command.loteId())
                    .filter(value -> proveedor.getId().equals(value.getProveedorId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado para el proveedor"));
            if (monto.compareTo(lote.getSaldoPendienteLote()) > 0) {
                throw new BusinessRuleException("El pago no puede superar el saldo pendiente del lote");
            }
            aplicarPagoALote(lote, monto, tipo, command.observaciones(), now);
        } else {
            BigDecimal restante = monto;
            for (Lote lote : loteRepository.findPendientesByProveedorId(proveedor.getId())) {
                if (restante.signum() == 0) {
                    break;
                }
                BigDecimal aplicado = restante.min(lote.getSaldoPendienteLote());
                aplicarPagoALote(lote, aplicado, tipo, command.observaciones(), now);
                restante = restante.subtract(aplicado);
            }
            if (restante.signum() > 0) {
                throw new BusinessRuleException("Los lotes pendientes no cubren el monto del pago");
            }
        }

        proveedor.setTotalPagado(proveedor.getTotalPagado().add(monto));
        actualizarSaldo(proveedor, now);
        return proveedorRepository.save(proveedor);
    }

    private void aplicarPagoALote(Lote lote, BigDecimal monto, TipoTransaccionProveedor tipo, String observaciones,
                                  Instant now) {
        lote.setMontoPagadoLote(lote.getMontoPagadoLote().add(monto));
        lote.setSaldoPendienteLote(lote.getCostoTotalLote().subtract(lote.getMontoPagadoLote()).max(ZERO));
        lote.setEstadoPago(estadoPago(lote.getMontoPagadoLote(), lote.getSaldoPendienteLote()));
        loteRepository.save(lote);
        registrarTransaccion(lote.getProveedorId(), lote.getId(), tipo, monto, observaciones, now);
    }

    private void registrarTransaccion(String proveedorId, String loteId, TipoTransaccionProveedor tipo,
                                      BigDecimal monto, String observaciones, Instant fecha) {
        transaccionRepository.save(TransaccionProveedor.builder()
                .proveedorId(proveedorId)
                .loteId(loteId)
                .tipo(tipo)
                .monto(monto)
                .fecha(fecha)
                .observaciones(observaciones)
                .build());
    }

    private void actualizarSaldo(Proveedor proveedor, Instant now) {
        proveedor.setSaldoPendiente(proveedor.getTotalDeuda().subtract(proveedor.getTotalPagado()).max(ZERO));
        proveedor.setUltimaActualizacion(now);
    }

    private EstadoPagoLote estadoPago(BigDecimal pagado, BigDecimal saldo) {
        if (saldo.signum() == 0) {
            return EstadoPagoLote.PAGADO_TOTAL;
        }
        return pagado.signum() > 0 ? EstadoPagoLote.PAGO_PARCIAL : EstadoPagoLote.PENDIENTE;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}

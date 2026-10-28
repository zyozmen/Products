package com.zyozmen.products.adapter.out.mongodb.mapper;

import com.zyozmen.products.adapter.out.mongodb.document.LoteDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProviderDocument;
import com.zyozmen.products.adapter.out.mongodb.document.TransaccionProveedorDocument;
import com.zyozmen.products.domain.model.LotePaymentState;
import com.zyozmen.products.domain.model.Container;
import com.zyozmen.products.domain.model.ProviderState;
import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.model.Presentation;
import com.zyozmen.products.domain.model.Provider;
import com.zyozmen.products.domain.model.ProviderTransactionType;
import com.zyozmen.products.domain.model.ProviderTransaction;
import com.zyozmen.products.domain.model.InventoryPresentation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ProviderMongoMapper {

    public ProviderDocument toDocument(Provider p) {
        return ProviderDocument.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .nit(p.getNit())
                .telefono(p.getTelefono()).correo(p.getEmail())
                .financiero(ProviderDocument.FinancieroDocument.builder()
                        .totalDeuda(p.getTotalDeuda()).totalPagado(p.getTotalPagado())
                        .saldoPendiente(p.getSaldoPendiente()).build())
                .estado(p.getEstado().name())
                .fechaCreacion(p.getFechaCreacion())
                .ultimaActualizacion(p.getUltimaActualizacion())
                .version(p.getVersion())
                .build();
    }

    public Provider toDomain(ProviderDocument d) {
        if (d.getFinanciero() == null) {
            throw new IllegalStateException("Proveedor sin financiero: ejecute la migracion de esquema HU-03");
        }
        return Provider.builder()
                .id(d.getId())
                .nombre(d.getNombre())
                .nit(d.getNit())
                .telefono(d.getTelefono()).email(d.getCorreo())
                .totalDeuda(d.getFinanciero().getTotalDeuda())
                .totalPagado(d.getFinanciero().getTotalPagado())
                .saldoPendiente(d.getFinanciero().getSaldoPendiente())
                .estado(d.getEstado() == null ? ProviderState.ACTIVO : ProviderState.valueOf(d.getEstado()))
                .fechaCreacion(d.getFechaCreacion())
                .ultimaActualizacion(d.getUltimaActualizacion())
                .version(d.getVersion())
                .build();
    }

    public LoteDocument toDocument(Lote l) {
        return LoteDocument.builder()
                .id(l.getId())
                .proveedorId(l.getProveedorId())
                .codigoLote(l.getCodigoLote())
                .costoTotalLote(l.getCostoTotalLote())
                .montoPagadoLote(l.getMontoPagadoLote())
                .saldoPendienteLote(l.getSaldoPendienteLote())
                .estadoPago(l.getEstadoPago().name())
                .estado("RECIBIDO".equals(l.getEstado()) ? "EN_RECEPCION" : l.getEstado())
                .contenedores(l.getContenedores() == null ? List.of() : l.getContenedores().stream()
                        .map(c -> LoteDocument.ContenedorDocument.builder()
                                .id(c.getId()).unidadBase(c.getUnidadBase())
                                .codigoContenedor(c.getCodigoContenedor())
                                .presentacionId(c.getPresentacionId())
                                .capacidadUnidadesBase(c.getCapacidadUnidadesBase())
                                .cantidadDisponible(c.getCantidadDisponible())
                                .cepa(LoteDocument.CepaDocument.builder().id(c.getCepaId()).nombre(c.getTipoCepa())
                                        .costoBaseUnidad(c.getCostoBasePorUnidad())
                                        .precioBaseUnidad(c.getPrecioBaseUnidad()).build())
                                .presentaciones(c.getPresentaciones().stream().map(this::toDocument).toList())
                                .subtotal(c.getSubtotal()).estado(c.getEstado()).build()).toList())
                .presentaciones(l.getPresentaciones() == null ? null : l.getPresentaciones().stream()
                        .map(p -> LoteDocument.PresentacionDocument.builder()
                        .presentacionId(p.getPresentacionId())
                        .cantidadUnidades(p.getCantidadUnidades())
                        .costoUnitario(p.getCostoUnitario())
                        .subtotal(p.getSubtotal())
                        .build()).toList())
                .fechaIngreso(l.getFechaIngreso())
                .abonoInicial(l.getAbonoInicial()).costoTotalEmpaquetado(l.getCostoTotalEmpaquetado())
                .createdAt(l.getCreatedAt()).updatedAt(l.getUpdatedAt()).version(l.getVersion())
                .build();
    }

    public Lote toDomain(LoteDocument d) {
        if (d.getCostoTotalLote() == null) {
            throw new IllegalStateException("Lote sin costoTotalCalculado: ejecute la migracion de esquema HU-03");
        }
        BigDecimal pagado = d.getMontoPagadoLote() != null ? d.getMontoPagadoLote()
                : d.getAbonoInicial() == null ? BigDecimal.ZERO : d.getAbonoInicial();
        BigDecimal saldo = d.getSaldoPendienteLote() != null ? d.getSaldoPendienteLote()
                : d.getCostoTotalLote().subtract(pagado);
        LotePaymentState estadoPago = d.getEstadoPago() != null ? LotePaymentState.valueOf(d.getEstadoPago())
                : saldo.signum() == 0 ? LotePaymentState.PAGADO_TOTAL
                : pagado.signum() > 0 ? LotePaymentState.PAGO_PARCIAL : LotePaymentState.PENDIENTE;
        return Lote.builder()
                .id(d.getId())
                .proveedorId(d.getProveedorId())
                .codigoLote(d.getCodigoLote())
                .costoTotalLote(d.getCostoTotalLote())
                .montoPagadoLote(pagado)
                .saldoPendienteLote(saldo)
                .estadoPago(estadoPago)
                .estado("EN_RECEPCION".equals(d.getEstado()) ? "RECIBIDO" : d.getEstado())
                .contenedores(d.getContenedores() == null ? List.of() : d.getContenedores().stream()
                        .map(c -> Container.builder()
                                .id(c.getId()).tipoCepa(c.getCepa().getNombre()).unidadBase(c.getUnidadBase())
                                .cepaId(c.getCepa().getId())
                                .codigoContenedor(c.getCodigoContenedor())
                                .presentacionId(c.getPresentacionId())
                                .capacidadUnidadesBase(c.getCapacidadUnidadesBase())
                                .cantidadDisponible(c.getCantidadDisponible() != null ? c.getCantidadDisponible()
                                        : c.getCapacidadUnidadesBase().subtract(c.getPresentaciones() == null ? BigDecimal.ZERO
                                        : c.getPresentaciones().stream().map(p -> p.contenidoUnidad()
                                                .multiply(BigDecimal.valueOf(p.unidadesCreadas())))
                                                .reduce(BigDecimal.ZERO, BigDecimal::add)))
                                .costoBasePorUnidad(c.getCepa().getCostoBaseUnidad())
                                .precioBaseUnidad(c.getCepa().getPrecioBaseUnidad())
                                .presentaciones(c.getPresentaciones() == null ? List.of()
                                        : c.getPresentaciones().stream().map(this::toDomain).toList())
                                .subtotal(c.getSubtotal() != null ? c.getSubtotal() : c.getCapacidadUnidadesBase()
                                        .multiply(c.getCepa().getCostoBaseUnidad()).setScale(2, RoundingMode.HALF_UP))
                                .estado(c.getEstado()).build()).toList())
                .presentaciones(d.getPresentaciones() == null ? null : d.getPresentaciones().stream()
                        .map(p -> Presentation.builder()
                        .presentacionId(p.getPresentacionId())
                        .cantidadUnidades(p.getCantidadUnidades())
                        .costoUnitario(p.getCostoUnitario())
                        .subtotal(p.getSubtotal())
                        .build()).toList())
                .fechaIngreso(d.getFechaIngreso())
                .abonoInicial(d.getAbonoInicial()).costoTotalEmpaquetado(d.getCostoTotalEmpaquetado())
                .createdAt(d.getCreatedAt()).updatedAt(d.getUpdatedAt()).version(d.getVersion())
                .build();
    }

    private LoteDocument.PresentacionInventarioDocument toDocument(InventoryPresentation p) {
        return new LoteDocument.PresentacionInventarioDocument(p.presentacionId(), p.sku(), p.nombre(),
                p.contenidoUnidad(), p.unidadesCreadas(), p.stockDisponible(), p.costoPresentacionCalculado(),
                p.precioVenta(), p.activoMarketplace(), p.confirmacionMargenNegativo(),
                ImageMongoMapper.toDocument(p.fotografia()));
    }

    private InventoryPresentation toDomain(LoteDocument.PresentacionInventarioDocument p) {
        return new InventoryPresentation(p.presentacionId(), p.sku(), p.nombre(),
                p.contenidoUnidad(), p.unidadesCreadas(), p.stockDisponible(), p.costoPresentacionCalculado(),
                p.precioVenta(), p.activoMarketplace(), p.confirmacionMargenNegativo(),
                ImageMongoMapper.toDomain(p.fotografia()));
    }

    public TransaccionProveedorDocument toDocument(ProviderTransaction t) {
        return TransaccionProveedorDocument.builder()
                .id(t.getId())
                .proveedorId(t.getProveedorId())
                .loteId(t.getLoteId())
                .tipo(t.getTipo().name())
                .monto(t.getMonto())
                .fecha(t.getFecha())
                .observaciones(t.getObservaciones())
                .build();
    }

    public ProviderTransaction toDomain(TransaccionProveedorDocument d) {
        return ProviderTransaction.builder()
                .id(d.getId())
                .proveedorId(d.getProveedorId())
                .loteId(d.getLoteId())
                .tipo(ProviderTransactionType.valueOf(d.getTipo()))
                .monto(d.getMonto())
                .fecha(d.getFecha())
                .observaciones(d.getObservaciones())
                .build();
    }
}

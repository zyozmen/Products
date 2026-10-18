package com.zyozmen.products.adapter.out.mongodb.mapper;

import com.zyozmen.products.adapter.out.mongodb.document.LoteDocument;
import com.zyozmen.products.adapter.out.mongodb.document.ProveedorDocument;
import com.zyozmen.products.adapter.out.mongodb.document.TransaccionProveedorDocument;
import com.zyozmen.products.domain.model.EstadoPagoLote;
import com.zyozmen.products.domain.model.EstadoProveedor;
import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.model.Presentacion;
import com.zyozmen.products.domain.model.Proveedor;
import com.zyozmen.products.domain.model.TipoTransaccionProveedor;
import com.zyozmen.products.domain.model.TransaccionProveedor;
import org.springframework.stereotype.Component;

@Component
public class ProveedorMongoMapper {

    public ProveedorDocument toDocument(Proveedor p) {
        return ProveedorDocument.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .nit(p.getNit())
                .contacto(ProveedorDocument.ContactoDocument.builder()
                        .telefono(p.getTelefono()).email(p.getEmail()).build())
                .totalDeuda(p.getTotalDeuda())
                .totalPagado(p.getTotalPagado())
                .saldoPendiente(p.getSaldoPendiente())
                .estado(p.getEstado().name())
                .fechaCreacion(p.getFechaCreacion())
                .ultimaActualizacion(p.getUltimaActualizacion())
                .version(p.getVersion())
                .build();
    }

    public Proveedor toDomain(ProveedorDocument d) {
        ProveedorDocument.ContactoDocument contacto = d.getContacto();
        return Proveedor.builder()
                .id(d.getId())
                .nombre(d.getNombre())
                .nit(d.getNit())
                .telefono(contacto == null ? null : contacto.getTelefono())
                .email(contacto == null ? null : contacto.getEmail())
                .totalDeuda(d.getTotalDeuda())
                .totalPagado(d.getTotalPagado())
                .saldoPendiente(d.getSaldoPendiente())
                .estado(EstadoProveedor.valueOf(d.getEstado()))
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
                .presentaciones(l.getPresentaciones().stream().map(p -> LoteDocument.PresentacionDocument.builder()
                        .presentacionId(p.getPresentacionId())
                        .cantidadUnidades(p.getCantidadUnidades())
                        .costoUnitario(p.getCostoUnitario())
                        .subtotal(p.getSubtotal())
                        .build()).toList())
                .fechaIngreso(l.getFechaIngreso())
                .build();
    }

    public Lote toDomain(LoteDocument d) {
        return Lote.builder()
                .id(d.getId())
                .proveedorId(d.getProveedorId())
                .codigoLote(d.getCodigoLote())
                .costoTotalLote(d.getCostoTotalLote())
                .montoPagadoLote(d.getMontoPagadoLote())
                .saldoPendienteLote(d.getSaldoPendienteLote())
                .estadoPago(EstadoPagoLote.valueOf(d.getEstadoPago()))
                .presentaciones(d.getPresentaciones().stream().map(p -> Presentacion.builder()
                        .presentacionId(p.getPresentacionId())
                        .cantidadUnidades(p.getCantidadUnidades())
                        .costoUnitario(p.getCostoUnitario())
                        .subtotal(p.getSubtotal())
                        .build()).toList())
                .fechaIngreso(d.getFechaIngreso())
                .build();
    }

    public TransaccionProveedorDocument toDocument(TransaccionProveedor t) {
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

    public TransaccionProveedor toDomain(TransaccionProveedorDocument d) {
        return TransaccionProveedor.builder()
                .id(d.getId())
                .proveedorId(d.getProveedorId())
                .loteId(d.getLoteId())
                .tipo(TipoTransaccionProveedor.valueOf(d.getTipo()))
                .monto(d.getMonto())
                .fecha(d.getFecha())
                .observaciones(d.getObservaciones())
                .build();
    }
}

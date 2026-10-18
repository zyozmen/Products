package com.zyozmen.products.domain.port.in;

import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.model.Proveedor;
import com.zyozmen.products.domain.model.TipoTransaccionProveedor;

import java.math.BigDecimal;
import java.util.List;

public interface ProveedorUseCase {

    Proveedor crear(CrearProveedorCommand command);

    Proveedor obtener(String id);

    Lote recibirLote(RecibirLoteCommand command);

    /** Registra un abono posterior; si loteId es nulo se aplica a los lotes pendientes, del más antiguo al más reciente. */
    Proveedor registrarPago(RegistrarPagoCommand command);

    record CrearProveedorCommand(String nombre, String nit, String telefono, String email) {
    }

    record PresentacionCommand(String presentacionId, int cantidadUnidades, BigDecimal costoUnitario) {
    }

    record RecibirLoteCommand(String proveedorId, String codigoLote, List<PresentacionCommand> presentaciones,
                              BigDecimal abonoInicial) {
    }

    record RegistrarPagoCommand(String proveedorId, String loteId, BigDecimal monto,
                                TipoTransaccionProveedor tipo, String observaciones) {
    }
}

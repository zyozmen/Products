package com.zyozmen.products.domain.port.in;

import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.model.Proveedor;
import com.zyozmen.products.domain.model.TipoTransaccionProveedor;
import com.zyozmen.products.domain.model.TransaccionProveedor;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;

public interface ProveedorUseCase {

    Proveedor crear(CrearProveedorCommand command);

    Proveedor obtener(String id);

    DetalleProveedor obtenerDetalle(String id);

    Page<Proveedor> buscar(String buscar, int page, int size);

    Lote recibirLote(RecibirLoteCommand command);

    Lote obtenerLote(String id);

    Page<Lote> listarLotes(String proveedorId, String buscar, int page, int size, String sort);

    Page<TransaccionProveedor> listarAbonos(String proveedorId, int page, int size);

    /** Registra un abono posterior; si loteId es nulo se aplica a los lotes pendientes, del más antiguo al más reciente. */
    Proveedor registrarPago(RegistrarPagoCommand command);

    record CrearProveedorCommand(String nombre, String nit, String telefono, String email) {
    }

    record DetalleProveedor(Proveedor proveedor, long lotesPagados, long lotesPorPagar) {
    }

    record ContenedorCommand(String cepaId, String unidadBase, String presentacionId,
                             BigDecimal capacidadUnidadesBase, BigDecimal costoBasePorUnidad,
                             BigDecimal precioBaseUnidad) {
        public ContenedorCommand(String cepaId, String unidadBase, String presentacionId,
                                 BigDecimal capacidadUnidadesBase, BigDecimal costoBasePorUnidad) {
            this(cepaId, unidadBase, presentacionId, capacidadUnidadesBase, costoBasePorUnidad, null);
        }
    }

    record RecibirLoteCommand(String proveedorId, List<ContenedorCommand> contenedores,
                              BigDecimal abonoInicial) {
    }

    record RegistrarPagoCommand(String proveedorId, String loteId, BigDecimal monto,
                                TipoTransaccionProveedor tipo, String observaciones) {
    }
}

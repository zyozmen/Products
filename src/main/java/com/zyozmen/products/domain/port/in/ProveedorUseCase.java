package com.zyozmen.products.domain.port.in;

import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.model.Provider;
import com.zyozmen.products.domain.model.ProviderTransactionType;
import com.zyozmen.products.domain.model.ProviderTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;

public interface ProveedorUseCase {

    Provider crear(CrearProveedorCommand command);

    Provider obtener(String id);

    DetalleProveedor obtenerDetalle(String id);

    Page<Provider> buscar(String buscar, int page, int size);

    Lote recibirLote(RecibirLoteCommand command);

    Lote obtenerLote(String id);

    Page<Lote> listarLotes(String proveedorId, String buscar, int page, int size, String sort);

    Page<ProviderTransaction> listarAbonos(String proveedorId, int page, int size);

    /** Registra un abono posterior; si loteId es nulo se aplica a los lotes pendientes, del más antiguo al más reciente. */
    Provider registrarPago(RegistrarPagoCommand command);

    /** Registra un abono vinculado a una jornada de liquidacion (fechaLiquidacion obligatoria). */
    ResultadoPago registrarPagoVinculado(RegistrarPagoCommand command);

    record CrearProveedorCommand(String nombre, String nit, String telefono, String email) {
    }

    record DetalleProveedor(Provider proveedor, long lotesPagados, long lotesPorPagar) {
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
                                ProviderTransactionType tipo, String observaciones, LocalDate fechaLiquidacion) {
        public RegistrarPagoCommand(String proveedorId, String loteId, BigDecimal monto,
                                    ProviderTransactionType tipo, String observaciones) {
            this(proveedorId, loteId, monto, tipo, observaciones, null);
        }
    }

    record AsignacionLote(String loteId, BigDecimal monto) {
    }

    record ResultadoPago(Provider proveedor, String correlationId, LocalDate fechaLiquidacion, BigDecimal monto,
                         List<AsignacionLote> asignaciones) {
    }
}

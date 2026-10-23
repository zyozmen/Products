package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.BusinessRuleException;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.EstadoPagoLote;
import com.zyozmen.products.domain.model.EstadoProveedor;
import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.model.Contenedor;
import com.zyozmen.products.domain.model.UnidadMedida;
import org.bson.types.ObjectId;
import com.zyozmen.products.domain.model.Proveedor;
import com.zyozmen.products.domain.model.TipoTransaccionProveedor;
import com.zyozmen.products.domain.model.TransaccionProveedor;
import com.zyozmen.products.domain.port.in.ProveedorUseCase;
import com.zyozmen.products.domain.port.in.CepaUseCase;
import com.zyozmen.products.domain.port.out.CodigoInventarioGeneratorPort;
import com.zyozmen.products.domain.port.out.LoteRepositoryPort;
import com.zyozmen.products.domain.port.out.ProveedorRepositoryPort;
import com.zyozmen.products.domain.port.out.TransaccionProveedorRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProveedorService implements ProveedorUseCase {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final String PROVEEDOR_NO_ENCONTRADO = "Proveedor no encontrado";

    private final ProveedorRepositoryPort proveedorRepository;
    private final LoteRepositoryPort loteRepository;
    private final TransaccionProveedorRepositoryPort transaccionRepository;
    private final CodigoInventarioGeneratorPort codigoInventarioGenerator;
    private final CepaUseCase cepas;

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
    public DetalleProveedor obtenerDetalle(String id) {
        Proveedor proveedor = obtener(id);
        return new DetalleProveedor(proveedor,
                loteRepository.countPagadosByProveedorId(proveedor.getId()),
                loteRepository.countPorPagarByProveedorId(proveedor.getId()));
    }

    @Override
    public Page<Proveedor> buscar(String buscar, int page, int size) {
        var pageable = crearPaginacion(page, size);
        String texto = buscar == null ? "" : buscar.strip();
        return proveedorRepository.buscar(texto, pageable);
    }

    @Override
    @Transactional
    public Lote recibirLote(RecibirLoteCommand command) {
        Proveedor proveedor = obtener(command.proveedorId());
        if (proveedor.getEstado() != EstadoProveedor.ACTIVO) {
            throw new BusinessRuleException("El proveedor no estÃ¡ activo");
        }
        if (command.contenedores() == null || command.contenedores().isEmpty()) {
            throw new BusinessRuleException(
                    "Debe agregar al menos un contenedor con su capacidad en unidades base para registrar el lote");
        }

        List<Contenedor> contenedores = new ArrayList<>();
        BigDecimal costoTotal = ZERO;
        for (ContenedorCommand item : command.contenedores()) {
            validarContenedor(item);
            var cepa = cepas.obtener(item.cepaId());
            BigDecimal costoUnitario = money(item.costoBasePorUnidad());
            BigDecimal subtotal = costoUnitario.multiply(item.capacidadUnidadesBase())
                    .setScale(2, RoundingMode.HALF_UP);
            contenedores.add(Contenedor.builder()
                    .id(new ObjectId().toHexString())
                    .codigoContenedor(formatearCodigo("CONT-", codigoInventarioGenerator.siguienteNumeroContenedor()))
                    .tipoCepa(cepa.nombre())
                    .cepaId(cepa.id())
                    .unidadBase(UnidadMedida.desde(item.unidadBase()).name())
                    .presentacionId(item.presentacionId())
                    .capacidadUnidadesBase(item.capacidadUnidadesBase())
                    .cantidadDisponible(item.capacidadUnidadesBase())
                    .costoBasePorUnidad(costoUnitario)
                    .precioBaseUnidad(item.precioBaseUnidad() == null ? null : money(item.precioBaseUnidad()))
                    .subtotal(subtotal)
                    .estado("RECIBIDO")
                    .build());
            costoTotal = costoTotal.add(subtotal);
        }

        if (costoTotal.signum() <= 0) {
            throw new BusinessRuleException("El valor total del lote debe ser mayor que cero");
        }
        if (command.abonoInicial() != null) {
            validarMonto(command.abonoInicial(), true, "El abono inicial");
        }
        BigDecimal abono = command.abonoInicial() == null ? ZERO : money(command.abonoInicial());
        if (abono.compareTo(costoTotal) > 0) {
            NumberFormat formato = NumberFormat.getNumberInstance(Locale.US);
            formato.setMaximumFractionDigits(2);
            throw new BusinessRuleException("El abono inicial no puede ser mayor al valor total del lote ($"
                    + formato.format(costoTotal) + " COP)");
        }

        Instant now = Instant.now();
        BigDecimal saldoLote = costoTotal.subtract(abono);
        Lote lote = loteRepository.save(Lote.builder()
                .proveedorId(proveedor.getId())
                .codigoLote(formatearCodigo("LOT-", codigoInventarioGenerator.siguienteNumeroLote()))
                .costoTotalLote(costoTotal)
                .montoPagadoLote(abono)
                .abonoInicial(abono)
                .costoTotalEmpaquetado(ZERO)
                .saldoPendienteLote(saldoLote)
                .estadoPago(estadoPago(abono, saldoLote))
                .estado("RECIBIDO")
                .contenedores(contenedores)
                .fechaIngreso(now)
                .createdAt(now).updatedAt(now)
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

    private static String formatearCodigo(String prefijo, long numero) {
        return prefijo + String.format(Locale.ROOT, "%06d", numero);
    }

    @Override
    public Lote obtenerLote(String id) {
        return loteRepository.findLoteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado"));
    }

    @Override
    public Page<Lote> listarLotes(String proveedorId, String buscar, int page, int size, String sort) {
        var paginacion = crearPaginacion(page, size);
        String[] partes = sort == null ? new String[]{"fechaIngreso", "desc"} : sort.split(",", -1);
        if (partes.length != 2) {
            throw new IllegalArgumentException("El orden debe tener formato campo,asc o campo,desc");
        }
        String campo = partes[0].strip();
        if (!List.of("fechaIngreso", "codigoLote", "costoTotalLote", "saldoPendienteLote").contains(campo)) {
            throw new IllegalArgumentException(
                    "Campo de orden no permitido: use fechaIngreso, codigoLote, costoTotalLote o saldoPendienteLote");
        }
        String direccion = partes[1].strip();
        if (!"asc".equalsIgnoreCase(direccion) && !"desc".equalsIgnoreCase(direccion)) {
            throw new IllegalArgumentException("La direccion de orden debe ser asc o desc");
        }
        Sort.Direction orden = Sort.Direction.fromString(direccion);
        var pageable = PageRequest.of(paginacion.getPageNumber(), paginacion.getPageSize(),
                Sort.by(orden, campo).and(Sort.by(orden, "id")));
        Proveedor proveedor = obtener(proveedorId);
        return loteRepository.findLotesByProveedorId(proveedor.getId(), buscar == null ? "" : buscar.strip(), pageable);
    }

    @Override
    public Page<TransaccionProveedor> listarAbonos(String proveedorId, int page, int size) {
        var pageable = crearPaginacion(page, size);
        Proveedor proveedor = obtener(proveedorId);
        return transaccionRepository.findAbonosByProveedorId(proveedor.getId(), pageable);
    }

    @Override
    @Transactional
    public Proveedor registrarPago(RegistrarPagoCommand command) {
        TipoTransaccionProveedor tipo = command.tipo() == null ? TipoTransaccionProveedor.ABONO : command.tipo();
        if (tipo == TipoTransaccionProveedor.DEUDA_LOTE || tipo == TipoTransaccionProveedor.ABONO_INICIAL) {
            throw new BusinessRuleException("El tipo " + tipo + " no es vÃ¡lido para un pago posterior");
        }
        validarMonto(command.monto(), false, "El monto del pago");
        BigDecimal monto = money(command.monto());

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
            List<Lote> pendientes = loteRepository.findPendientesByProveedorId(proveedor.getId());
            BigDecimal saldoDisponible = pendientes.stream().map(Lote::getSaldoPendienteLote)
                    .reduce(ZERO, BigDecimal::add);
            if (monto.compareTo(saldoDisponible) > 0) {
                throw new BusinessRuleException("Los lotes pendientes no cubren el monto del pago");
            }
            BigDecimal restante = monto;
            for (Lote lote : pendientes) {
                if (restante.signum() == 0) {
                    break;
                }
                BigDecimal aplicado = restante.min(lote.getSaldoPendienteLote());
                aplicarPagoALote(lote, aplicado, tipo, command.observaciones(), now);
                restante = restante.subtract(aplicado);
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
        lote.setUpdatedAt(now);
        loteRepository.save(lote);
        registrarTransaccion(lote.getProveedorId(), lote.getId(), tipo, monto, observaciones, now);
    }

    private void registrarTransaccion(String proveedorId, String loteId, TipoTransaccionProveedor tipo,
                                      BigDecimal monto, String observaciones, Instant fecha) {
        transaccionRepository.insert(TransaccionProveedor.builder()
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

    private PageRequest crearPaginacion(int page, int size) {
        if (page < 0 || size < 1 || size > 200) {
            throw new IllegalArgumentException("La pagina debe ser >= 0 y el tamano debe estar entre 1 y 200");
        }
        return PageRequest.of(page, size);
    }

    private void validarContenedor(ContenedorCommand item) {
        if (item == null || item.cepaId() == null || item.cepaId().isBlank()
                || item.unidadBase() == null || item.unidadBase().isBlank()) {
            throw new BusinessRuleException("Cada contenedor debe indicar su tipo o cepa y su unidad base");
        }
        BigDecimal capacidad = item.capacidadUnidadesBase();
        if (capacidad == null || capacidad.signum() <= 0
                || capacidad.stripTrailingZeros().scale() > 3
                || capacidad.precision() - capacidad.scale() > 15) {
            throw new BusinessRuleException("La capacidad debe ser positiva y admitir maximo 15 enteros y 3 decimales");
        }
        validarMonto(item.costoBasePorUnidad(), false, "El costo base por unidad");
        UnidadMedida.desde(item.unidadBase());
        if (item.precioBaseUnidad() != null) {
            validarMonto(item.precioBaseUnidad(), true, "El precio base por unidad");
        }
    }

    private void validarMonto(BigDecimal monto, boolean permiteCero, String campo) {
        if (monto == null || monto.signum() < 0 || (!permiteCero && monto.signum() == 0)
                || monto.stripTrailingZeros().scale() > 2 || monto.precision() - monto.scale() > 15) {
            throw new BusinessRuleException(campo + (permiteCero ? " debe ser no negativo" : " debe ser mayor que cero")
                    + " y admitir maximo 15 enteros y 2 decimales");
        }
    }
}

package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.BusinessRuleException;
import com.zyozmen.products.domain.exception.CapacidadExcedidaException;
import com.zyozmen.products.domain.exception.MargenNegativoException;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.Contenedor;
import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.model.PresentacionInventario;
import com.zyozmen.products.domain.model.ProductoMarketplace;
import com.zyozmen.products.domain.model.FotografiaPresentacion;
import com.zyozmen.products.domain.port.in.PresentacionUseCase;
import com.zyozmen.products.domain.port.in.CepaUseCase;
import com.zyozmen.products.domain.port.out.LoteRepositoryPort;
import com.zyozmen.products.domain.port.out.MarketplaceRepositoryPort;
import com.zyozmen.products.domain.port.out.ProveedorRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PresentacionService implements PresentacionUseCase {
    private final LoteRepositoryPort loteRepository;
    private final ProveedorRepositoryPort proveedorRepository;
    private final MarketplaceRepositoryPort marketplaceRepository;
    private final CepaUseCase cepas;

    @Override
    @Transactional
    public Lote empaquetar(String loteId, String contenedorId, List<CrearPresentacionCommand> comandos) {
        if (comandos == null || comandos.isEmpty()) {
            throw new IllegalArgumentException("Debe agregar al menos una presentacion");
        }
        Lote lote = loteRepository.findLoteById(loteId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado"));
        Contenedor contenedor = lote.getContenedores().stream()
                .filter(c -> c.getId().equals(contenedorId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Contenedor no encontrado en el lote"));
        if (contenedor.getCepaId() == null) {
            throw new BusinessRuleException("Contenedor sin cepaId: ejecute la reconciliacion de cepas");
        }
        var cepa = cepas.obtener(contenedor.getCepaId());
        var proveedor = proveedorRepository.findById(lote.getProveedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));

        var ids = new HashSet<String>();
        var skus = new HashSet<String>();
        lote.getContenedores().forEach(c -> c.getPresentaciones().forEach(p -> {
            ids.add(p.presentacionId());
            skus.add(p.sku());
        }));
        List<PresentacionInventario> nuevas = new ArrayList<>();
        BigDecimal total = contenidoEmpaquetado(contenedor.getPresentaciones());
        for (CrearPresentacionCommand command : comandos) {
            validar(command);
            String id = command.presentacionId() == null ? new ObjectId().toHexString()
                    : new ObjectId(command.presentacionId()).toHexString();
            String sku = command.sku().strip();
            if (!ids.add(id) || !skus.add(sku) || marketplaceRepository.existsByPresentacionId(id)
                    || marketplaceRepository.existsBySku(sku)) {
                throw new BusinessRuleException("Ya existe una presentacion con ese identificador o SKU");
            }
            BigDecimal costo = command.contenidoUnidad().multiply(contenedor.getCostoBasePorUnidad())
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal precio = command.precioVenta().setScale(2, RoundingMode.UNNECESSARY);
            if (precio.compareTo(costo) < 0 && !command.confirmacionMargenNegativo()) {
                throw new MargenNegativoException("Atención: El precio de venta (" + numero(precio)
                        + ") es menor al costo base de la presentación (" + numero(costo) + ")");
            }
            total = total.add(command.contenidoUnidad().multiply(BigDecimal.valueOf(command.unidadesCreadas())));
            nuevas.add(new PresentacionInventario(id, sku, command.nombre().strip(), command.contenidoUnidad(),
                    command.unidadesCreadas(), command.unidadesCreadas(), costo, precio,
                    command.activoMarketplace(), command.confirmacionMargenNegativo(),
                    FotografiaPresentacion.validar(command.fotografia())));
        }
        if (total.compareTo(contenedor.getCapacidadUnidadesBase()) > 0) {
            throw new CapacidadExcedidaException("El total empaquetado (" + numero(total) + contenedor.getUnidadBase()
                    + ") supera la capacidad máxima del contenedor (" + numero(contenedor.getCapacidadUnidadesBase())
                    + contenedor.getUnidadBase() + ")");
        }
        var presentaciones = new ArrayList<>(contenedor.getPresentaciones());
        presentaciones.addAll(nuevas);
        Contenedor actualizado = contenedor.toBuilder().presentaciones(List.copyOf(presentaciones))
                .cantidadDisponible(contenedor.getCapacidadUnidadesBase().subtract(total)).build();
        lote.setContenedores(lote.getContenedores().stream()
                .map(c -> c.getId().equals(contenedorId) ? actualizado : c).toList());
        lote.setCostoTotalEmpaquetado(lote.getContenedores().stream().flatMap(c -> c.getPresentaciones().stream())
                .map(p -> p.costoPresentacionCalculado().multiply(BigDecimal.valueOf(p.unidadesCreadas())))
                .reduce(new BigDecimal("0.00"), BigDecimal::add));
        lote.setEstado("PROCESADO");
        lote.setUpdatedAt(Instant.now());
        Lote guardado = loteRepository.save(lote);
        for (PresentacionInventario p : nuevas) {
            marketplaceRepository.insert(new ProductoMarketplace(p.presentacionId(), lote.getId(),
                    proveedor.getNombre(), cepa.nombre(), cepa.nombre(),
                    p.sku(), p.precioVenta(), p.stockDisponible(), p.fotografia(),
                    p.activoMarketplace() ? "ACTIVO" : "INACTIVO", cepa.id(), p.nombre(),
                    p.contenidoUnidad(), contenedor.getUnidadBase()));
        }
        return guardado;
    }

    private BigDecimal contenidoEmpaquetado(List<PresentacionInventario> presentaciones) {
        return presentaciones.stream().map(p -> p.contenidoUnidad().multiply(BigDecimal.valueOf(p.unidadesCreadas())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validar(CrearPresentacionCommand p) {
        if (p == null || p.sku() == null || p.sku().isBlank() || p.nombre() == null || p.nombre().isBlank()
                || p.unidadesCreadas() < 1 || !decimalValido(p.contenidoUnidad(), 3, false)
                || !decimalValido(p.precioVenta(), 2, true)
                || (p.presentacionId() != null && !ObjectId.isValid(p.presentacionId()))) {
            throw new IllegalArgumentException("Presentacion invalida: revise identificador, SKU, nombre, contenido, unidades y precio");
        }
    }

    private boolean decimalValido(BigDecimal valor, int decimales, boolean permiteCero) {
        return valor != null && valor.signum() >= (permiteCero ? 0 : 1)
                && valor.stripTrailingZeros().scale() <= decimales && valor.precision() - valor.scale() <= 15;
    }

    private String numero(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }
}

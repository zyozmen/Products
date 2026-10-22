package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.OrderProductException;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.model.ProductoMarketplace;
import com.zyozmen.products.domain.model.PresentacionInventario;
import com.zyozmen.products.domain.model.TrazabilidadPresentacion;
import com.zyozmen.products.domain.port.in.MarketplaceUseCase;
import com.zyozmen.products.domain.port.out.LoteRepositoryPort;
import com.zyozmen.products.domain.port.out.MarketplaceRepositoryPort;
import com.zyozmen.products.domain.port.in.CepaUseCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class MarketplaceService implements MarketplaceUseCase {
    private final MarketplaceRepositoryPort marketplace;
    private final LoteRepositoryPort lotes;
    private final CepaUseCase cepas;

    @Override
    public PresentacionVenta resolverPresentacion(String presentacionId) {
        var producto = obtenerActivo(presentacionId);
        var origen = resolverOrigen(producto);
        return new PresentacionVenta(origen.nombre(), producto.precioVenta(), origen.trazabilidad());
    }

    @Override
    public DetalleProducto obtenerDetalle(String cepaId, String presentacionId) {
        var seleccionada = obtenerActivo(presentacionId);
        if (!cepaId.equals(seleccionada.cepaId()) || seleccionada.stockDisponible() <= 0) {
            throw new ResourceNotFoundException("La presentacion no pertenece a la cepa indicada");
        }
        var cepa = cepas.obtener(cepaId);
        var presentaciones = marketplace.findDisponiblesByCepaId(cepaId).stream()
                .map(p -> {
                    var origen = resolverOrigen(p);
                    return new PresentacionDetalle(p.presentacionId(), p.sku(), origen.presentacion().nombre(),
                            origen.trazabilidad().contenidoUnidad(), origen.trazabilidad().unidadBase(),
                            p.precioVenta(), p.stockDisponible(), p.fotografia());
                })
                .sorted(Comparator.comparing(PresentacionDetalle::contenidoUnidad)
                        .thenComparing(PresentacionDetalle::sku))
                .toList();
        return new DetalleProducto(cepa.id(), cepa.nombre(), presentacionId, presentaciones, cepa.fotografia(),
                presentaciones.stream().map(PresentacionDetalle::precioVenta).min(Comparator.naturalOrder())
                        .orElseThrow(() -> new ResourceNotFoundException("Cepa sin presentaciones disponibles")));
    }

    @Override
    public Page<ProductoCatalogo> listar(String buscar, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page debe ser >= 0 y size entre 1 y 100");
        }
        String texto = buscar == null ? "" : buscar.strip();
        if (texto.length() > 100) {
            throw new IllegalArgumentException("La busqueda admite hasta 100 caracteres");
        }
        return marketplace.buscarCatalogo(texto, PageRequest.of(page, size));
    }

    private ProductoMarketplace obtenerActivo(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El ID de presentacion es obligatorio");
        }
        return marketplace.findByPresentacionId(id)
                .filter(p -> "ACTIVO".equals(p.estado()))
                .orElseThrow(() -> new ResourceNotFoundException("Presentacion no encontrada o inactiva"));
    }

    private Origen resolverOrigen(ProductoMarketplace producto) {
        var lote = lotes.findLoteById(producto.loteId())
                .orElseThrow(() -> new OrderProductException("La presentacion no tiene un lote valido"));
        if (lote.getProveedorId() == null || lote.getProveedorId().isBlank() || lote.getContenedores() == null) {
            throw new OrderProductException("La presentacion no tiene trazabilidad de proveedor");
        }
        for (var contenedor : lote.getContenedores()) {
            for (var presentacion : contenedor.getPresentaciones()) {
                if (!presentacion.presentacionId().equals(producto.presentacionId())) {
                    continue;
                }
                if (!presentacion.activoMarketplace()
                        || contenedor.getCepaId() == null
                        || !contenedor.getCepaId().equals(producto.cepaId())
                        || contenedor.getId() == null || contenedor.getUnidadBase() == null
                        || contenedor.getCostoBasePorUnidad() == null
                        || contenedor.getCostoBasePorUnidad().signum() < 0
                        || presentacion.contenidoUnidad() == null || presentacion.contenidoUnidad().signum() <= 0
                        || producto.precioVenta() == null || producto.precioVenta().signum() < 0) {
                    throw new OrderProductException("La presentacion tiene datos de venta o costo invalidos");
                }
                var costo = contenedor.getCostoBasePorUnidad().multiply(presentacion.contenidoUnidad())
                        .setScale(2, RoundingMode.HALF_UP);
                var trazabilidad = new TrazabilidadPresentacion(lote.getProveedorId(), lote.getId(),
                        contenedor.getId(), contenedor.getCepaId(),
                        presentacion.presentacionId(), presentacion.contenidoUnidad(), contenedor.getUnidadBase(),
                        contenedor.getCostoBasePorUnidad(), costo);
                return new Origen(cepas.obtener(contenedor.getCepaId()).nombre(), presentacion, trazabilidad);
            }
        }
        throw new OrderProductException("La presentacion no pertenece al lote publicado");
    }

    private record Origen(String nombre, PresentacionInventario presentacion,
                          TrazabilidadPresentacion trazabilidad) { }
}

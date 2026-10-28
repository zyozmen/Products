package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.OrderConflictException;
import com.zyozmen.products.domain.exception.OrderProductException;
import com.zyozmen.products.domain.model.OrderItem;
import com.zyozmen.products.domain.model.InventoryPresentation;
import com.zyozmen.products.domain.model.PresentationTrace;
import com.zyozmen.products.domain.port.out.InventarioOrdenPort;
import com.zyozmen.products.domain.port.out.LoteRepositoryPort;
import com.zyozmen.products.domain.port.out.MarketplaceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class InventarioOrdenService implements InventarioOrdenPort {
    private final LoteRepositoryPort lotes;
    private final MarketplaceRepositoryPort marketplace;

    @Override
    public void descontar(List<OrderItem> items) {
        aplicar(items, true);
    }

    @Override
    public void restituir(List<OrderItem> items) {
        aplicar(items, false);
    }

    private void aplicar(List<OrderItem> items, boolean descontar) {
        if (items == null || items.isEmpty()) {
            throw new OrderProductException("La orden no tiene items para mover inventario");
        }
        Map<String, Map<String, Movimiento>> movimientos = new TreeMap<>();
        for (var item : items) {
            var t = item == null ? null : item.getTrazabilidad();
            if (t == null || t.loteId() == null || t.contenedorId() == null || t.proveedorId() == null
                    || t.cepaId() == null || t.presentacionId() == null
                    || !t.presentacionId().equals(item.getProductId()) || item.getQuantity() <= 0) {
                throw new OrderProductException("El item no tiene trazabilidad valida para mover inventario");
            }
            var porPresentacion = movimientos.computeIfAbsent(t.loteId(), id -> new TreeMap<>());
            var anterior = porPresentacion.get(t.presentacionId());
            long cantidad = (long) item.getQuantity() + (anterior == null ? 0 : anterior.cantidad());
            if (cantidad > Integer.MAX_VALUE || (anterior != null && !anterior.origen().equals(t))) {
                throw new OrderProductException("Cantidad o trazabilidad incompatible en items repetidos");
            }
            porPresentacion.put(t.presentacionId(), new Movimiento(t, (int) cantidad));
        }
        for (var entry : movimientos.entrySet()) {
            var lote = lotes.findLoteById(entry.getKey())
                    .orElseThrow(() -> new OrderProductException("Lote de inventario no encontrado"));
            if (lote.getContenedores() == null) {
                throw new OrderProductException("El lote no tiene contenedores de inventario");
            }
            var contenedores = new ArrayList<>(lote.getContenedores());
            for (var movimiento : entry.getValue().values()) {
                var origen = movimiento.origen();
                int indice = -1;
                for (int i = 0; i < contenedores.size(); i++) {
                    if (origen.contenedorId().equals(contenedores.get(i).getId())) {
                        indice = i;
                        break;
                    }
                }
                if (indice < 0 || !origen.proveedorId().equals(lote.getProveedorId())) {
                    throw new OrderProductException("La procedencia del item no coincide con el lote");
                }
                var contenedor = contenedores.get(indice);
                if (!origen.cepaId().equals(contenedor.getCepaId())) {
                    throw new OrderProductException("La cepa del item no coincide con el contenedor");
                }
                if (contenedor.getPresentaciones() == null) {
                    throw new OrderProductException("El contenedor no tiene presentaciones de inventario");
                }
                var presentaciones = new ArrayList<>(contenedor.getPresentaciones());
                int posicion = -1;
                for (int i = 0; i < presentaciones.size(); i++) {
                    if (origen.presentacionId().equals(presentaciones.get(i).presentacionId())) {
                        posicion = i;
                        break;
                    }
                }
                if (posicion < 0) {
                    throw new OrderProductException("Presentacion no encontrada en el contenedor");
                }
                var p = presentaciones.get(posicion);
                long nuevo = (long) p.stockDisponible() + (descontar ? -movimiento.cantidad() : movimiento.cantidad());
                if (nuevo < 0 || nuevo > p.unidadesCreadas() || (descontar && !p.activoMarketplace())) {
                    throw new OrderConflictException("Stock insuficiente o presentacion no disponible: " + p.presentacionId());
                }
                marketplace.actualizarStock(p.presentacionId(), lote.getId(), p.stockDisponible(), (int) nuevo);
                presentaciones.set(posicion, new InventoryPresentation(p.presentacionId(), p.sku(), p.nombre(),
                        p.contenidoUnidad(), p.unidadesCreadas(), (int) nuevo, p.costoPresentacionCalculado(),
                        p.precioVenta(), p.activoMarketplace(), p.confirmacionMargenNegativo(), p.fotografia()));
                contenedores.set(indice, contenedor.toBuilder().presentaciones(List.copyOf(presentaciones)).build());
            }
            lote.setContenedores(List.copyOf(contenedores));
            lote.setUpdatedAt(Instant.now());
            lotes.save(lote);
        }
    }

    private record Movimiento(PresentationTrace origen, int cantidad) { }
}

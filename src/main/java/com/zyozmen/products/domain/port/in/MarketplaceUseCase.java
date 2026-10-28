package com.zyozmen.products.domain.port.in;

import com.zyozmen.products.domain.model.Image;
import com.zyozmen.products.domain.model.PresentationTrace;
import com.zyozmen.products.domain.model.MarketplaceCatalogFilter;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;

public interface MarketplaceUseCase {
    PresentacionVenta resolverPresentacion(String presentacionId);

    DetalleProducto obtenerDetalle(String cepaId, String presentacionId);

    Page<ProductoCatalogo> listar(String buscar, int page, int size);
    Page<ProductoCatalogo> listar(MarketplaceCatalogFilter filtro, int page, int size);

    record PresentacionVenta(String nombre, BigDecimal precioVenta, PresentationTrace trazabilidad) { }

    record DetalleProducto(String cepaId, String nombre, String presentacionSeleccionadaId,
                           List<PresentacionDetalle> presentaciones, Image fotografia, BigDecimal precioDesde) {
        public DetalleProducto(String cepaId, String nombre, String seleccionada, List<PresentacionDetalle> ps) {
            this(cepaId, nombre, seleccionada, ps, null, null);
        }
    }

    record ProductoCatalogo(String cepaId, String nombre, Image fotografia, BigDecimal precioDesde,
                            List<PresentacionDetalle> presentaciones) { }

    record PresentacionDetalle(String presentacionId, String sku, String nombre,
                               BigDecimal contenidoUnidad, String unidadBase, BigDecimal precioVenta,
                               int stockDisponible, Image fotografia) { }
}

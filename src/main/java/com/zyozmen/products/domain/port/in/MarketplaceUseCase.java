package com.zyozmen.products.domain.port.in;

import com.zyozmen.products.domain.model.Imagen;
import com.zyozmen.products.domain.model.TrazabilidadPresentacion;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;

public interface MarketplaceUseCase {
    PresentacionVenta resolverPresentacion(String presentacionId);

    DetalleProducto obtenerDetalle(String cepaId, String presentacionId);

    Page<ProductoCatalogo> listar(String buscar, int page, int size);

    record PresentacionVenta(String nombre, BigDecimal precioVenta, TrazabilidadPresentacion trazabilidad) { }

    record DetalleProducto(String cepaId, String nombre, String presentacionSeleccionadaId,
                           List<PresentacionDetalle> presentaciones, Imagen fotografia, BigDecimal precioDesde) {
        public DetalleProducto(String cepaId, String nombre, String seleccionada, List<PresentacionDetalle> ps) {
            this(cepaId, nombre, seleccionada, ps, null, null);
        }
    }

    record ProductoCatalogo(String cepaId, String nombre, Imagen fotografia, BigDecimal precioDesde,
                            List<PresentacionDetalle> presentaciones) { }

    record PresentacionDetalle(String presentacionId, String sku, String nombre,
                               BigDecimal contenidoUnidad, String unidadBase, BigDecimal precioVenta,
                               int stockDisponible, Imagen fotografia) { }
}

package com.zyozmen.products.domain.port.in;

import com.zyozmen.products.domain.model.Lote;
import com.zyozmen.products.domain.model.Imagen;

import java.math.BigDecimal;
import java.util.List;

public interface PresentacionUseCase {
    Lote empaquetar(String loteId, String contenedorId, List<CrearPresentacionCommand> presentaciones);

    record CrearPresentacionCommand(String presentacionId, String sku, String nombre,
                                    BigDecimal contenidoUnidad, int unidadesCreadas,
                                    BigDecimal precioVenta, boolean activoMarketplace,
                                    boolean confirmacionMargenNegativo, Imagen fotografia) {
    }
}

package com.zyozmen.products.domain.port.in;

import com.zyozmen.products.domain.model.Cepa;
import com.zyozmen.products.domain.model.Imagen;
import org.springframework.data.domain.Page;

public interface CepaUseCase {
    Cepa crear(String nombre);
    Cepa actualizar(String id, String nombre, Long version);
    Cepa obtener(String id);
    Page<Cepa> listar(int page, int size);
    Cepa guardarFotografia(String id, Imagen fotografia);
}

package com.zyozmen.products.domain.port.in;

import com.zyozmen.products.domain.model.Cepa;
import com.zyozmen.products.domain.model.Imagen;
import org.springframework.data.domain.Page;
import java.util.List;

public interface CepaUseCase {
    Cepa crear(String nombre);
    Cepa crear(String nombre, List<Long> categoryIds);
    Cepa actualizar(String id, String nombre, Long version);
    Cepa actualizar(String id, String nombre, Long version, List<Long> categoryIds);
    Cepa obtener(String id);
    Page<Cepa> listar(int page, int size);
    Cepa guardarFotografia(String id, Imagen fotografia);
}

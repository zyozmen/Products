package com.zyozmen.products.domain.port.in;

import com.zyozmen.products.domain.model.Product;
import com.zyozmen.products.domain.model.Image;
import org.springframework.data.domain.Page;
import java.util.List;

public interface ProductUseCase {
    Product crear(String nombre);
    Product crear(String nombre, List<Long> categoryIds);
    Product actualizar(String id, String nombre, Long version);
    Product actualizar(String id, String nombre, Long version, List<Long> categoryIds);
    Product obtener(String id);
    Page<Product> listar(int page, int size);
    Product guardarFotografia(String id, Image fotografia);
}

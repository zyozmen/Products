package com.zyozmen.products.domain.port.in;

import com.zyozmen.products.domain.model.Producto;

import java.util.List;

/**
 * Puerto de entrada (Inbound Port / Use Case).
 *
 * Define los casos de uso que el dominio expone hacia los adaptadores
 * de entrada (controladores, mensajería, CLI, etc.).
 * Al trabajar únicamente con el modelo de dominio, el núcleo queda
 * completamente desacoplado de los detalles de transporte HTTP.
 */
public interface ProductoUseCase {

    List<Producto> listarTodos();

    Producto obtenerPorId(Long id);

    Producto crear(Producto producto);

    Producto actualizar(Long id, Producto producto);

    void eliminar(Long id);
}

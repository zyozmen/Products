package com.zyozmen.products.adapter.in.graphql;

import com.zyozmen.products.adapter.in.web.dto.ProductoRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.ProductoResponseDTO;
import com.zyozmen.products.adapter.in.web.mapper.ProductoWebMapper;
import com.zyozmen.products.domain.port.in.ProductoUseCase;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductGraphqlAdapter {

    @Autowired
    private ProductoUseCase productoUseCase;
    @Autowired
    private ProductoWebMapper productoWebMapper;

    @QueryMapping
    public List<ProductoResponseDTO> listarTodos() {
        return productoUseCase.listarTodos()
                .stream()
                .map(productoWebMapper::toResponseDTO)
                .toList();
    }

    @QueryMapping
    public ProductoResponseDTO obtenerPorId(@Argument Long id) {
        return productoWebMapper.toResponseDTO(productoUseCase.obtenerPorId(id));
    }

    @MutationMapping
    public ProductoResponseDTO crear(@Argument("input") ProductoRequestDTO input) {
        return productoWebMapper.toResponseDTO(
                productoUseCase.crear(productoWebMapper.toDomain(input))
        );
    }

    @MutationMapping
    public ProductoResponseDTO actualizar(@Argument Long id, @Argument("input") ProductoRequestDTO input) {
        return productoWebMapper.toResponseDTO(
                productoUseCase.actualizar(id, productoWebMapper.toDomain(input))
        );
    }

    @MutationMapping
    public Boolean eliminar(@Argument Long id) {
        productoUseCase.eliminar(id);
        return true;
    }
}

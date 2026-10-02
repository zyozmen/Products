package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.UserResponseDTO;
import com.zyozmen.products.adapter.in.web.mapper.UserWebMapper;
import com.zyozmen.products.domain.port.in.UserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Adaptador de entrada REST (Inbound Adapter) para la administración de
 * usuarios. Protegido a nivel de Spring Security: solo accesible con
 * un JWT cuyo rol sea ADMIN (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Administración de Usuarios", description = "Gestión de usuarios (solo rol admin)")
public class AdminUserController {

    private final UserUseCase userUseCase;
    private final UserWebMapper userWebMapper;

    @Operation(summary = "Listar todos los usuarios del sistema")
    @ApiResponse(responseCode = "200", description = "Listado de usuarios obtenido exitosamente")
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> listarUsuarios() {
        List<UserResponseDTO> usuarios = userUseCase.listarUsuarios()
                .stream()
                .map(userWebMapper::toResponseDTO)
                .toList();
        return ResponseEntity.ok(usuarios);
    }

    @Operation(summary = "Activar/inactivar el estado de un usuario")
    @ApiResponse(responseCode = "200", description = "Estado del usuario actualizado exitosamente")
    @PutMapping("/{username}/toggle-status")
    public ResponseEntity<UserResponseDTO> toggleStatus(@PathVariable String username) {
        return ResponseEntity.ok(userWebMapper.toResponseDTO(userUseCase.toggleEstado(username)));
    }
}

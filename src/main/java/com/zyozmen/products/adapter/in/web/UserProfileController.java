package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.UpdateProfileRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.UserResponseDTO;
import com.zyozmen.products.adapter.in.web.mapper.UserWebMapper;
import com.zyozmen.products.domain.model.User;
import com.zyozmen.products.domain.port.in.UserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST (Inbound Adapter) para el perfil del usuario
 * autenticado. El username se obtiene del JWT (SecurityContext), por lo
 * que un usuario solo puede ver/editar su propio perfil.
 */
@RestController
@RequestMapping("/api/users/profile")
@RequiredArgsConstructor
@Tag(name = "Perfil de Usuario", description = "Consulta y actualización del perfil del usuario autenticado")
public class UserProfileController {

    private final UserUseCase userUseCase;
    private final UserWebMapper userWebMapper;

    @Operation(summary = "Obtener el perfil del usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Perfil obtenido exitosamente")
    @GetMapping
    public ResponseEntity<UserResponseDTO> obtenerPerfil(Authentication authentication) {
        User user = userUseCase.obtenerPorUsername(authentication.getName());
        return ResponseEntity.ok(userWebMapper.toResponseDTO(user));
    }

    @Operation(summary = "Actualizar el perfil del usuario autenticado",
            description = "Permite modificar nombre, apellido, dirección, teléfono y/o contraseña. "
                    + "Solo se actualizan los campos enviados.")
    @ApiResponse(responseCode = "200", description = "Perfil actualizado exitosamente")
    @PutMapping
    public ResponseEntity<UserResponseDTO> actualizarPerfil(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequestDTO request) {

        User user = userUseCase.actualizarPerfil(
                authentication.getName(),
                request.getNombre(),
                request.getApellido(),
                request.getDireccion(),
                request.getTelefono(),
                request.getPassword());

        return ResponseEntity.ok(userWebMapper.toResponseDTO(user));
    }
}

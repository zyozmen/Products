package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.LoginRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.LoginResponseDTO;
import com.zyozmen.products.adapter.in.web.dto.RegisterRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.UserResponseDTO;
import com.zyozmen.products.adapter.in.web.mapper.UserWebMapper;
import com.zyozmen.products.domain.model.AuthResult;
import com.zyozmen.products.domain.model.User;
import com.zyozmen.products.domain.port.in.UserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST (Inbound Adapter) para autenticación.
 * Expone los endpoints públicos de registro e inicio de sesión.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Registro e inicio de sesión de usuarios")
public class AuthController {

    private final UserUseCase userUseCase;
    private final UserWebMapper userWebMapper;

    @Operation(summary = "Registrar un nuevo usuario")
    @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente")
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        User user = userUseCase.registrar(userWebMapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(userWebMapper.toResponseDTO(user));
    }

    @Operation(summary = "Iniciar sesión")
    @ApiResponse(responseCode = "200", description = "Login exitoso, retorna token JWT y perfil")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResult authResult = userUseCase.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(userWebMapper.toLoginResponseDTO(authResult));
    }
}

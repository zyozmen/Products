package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.LoginRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.LoginResponseDTO;
import com.zyozmen.products.adapter.in.web.dto.LogoutRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.RegisterRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.UserResponseDTO;
import com.zyozmen.products.adapter.in.web.mapper.UserWebMapper;
import com.zyozmen.products.domain.exception.SessionExpiredException;
import com.zyozmen.products.domain.model.AuthResult;
import com.zyozmen.products.domain.model.LogoutReason;
import com.zyozmen.products.domain.model.User;
import com.zyozmen.products.domain.port.in.SessionUseCase;
import com.zyozmen.products.domain.port.in.UserUseCase;
import com.zyozmen.products.exception.SessionErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST (Inbound Adapter) para autenticación.
 * Expone los endpoints públicos de registro e inicio de sesión.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Registro, inicio de sesión, renovación y cierre de sesión")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserUseCase userUseCase;
    private final UserWebMapper userWebMapper;
    private final SessionUseCase sessionUseCase;

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

    @Operation(summary = "Renovar token",
            description = "Valida el token actual (firmado, no expirado, no revocado, sin inactividad) "
                    + "y emite uno nuevo con 1 hora de vigencia; el anterior queda revocado.")
    @ApiResponse(responseCode = "200", description = "Nuevo token emitido")
    @ApiResponse(responseCode = "401", description = "SESSION_EXPIRED",
            content = @Content(schema = @Schema(implementation = SessionErrorResponse.class)))
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refresh(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        AuthResult authResult = sessionUseCase.refresh(extractToken(authorization));
        return ResponseEntity.ok(userWebMapper.toLoginResponseDTO(authResult));
    }

    @Operation(summary = "Cerrar sesión",
            description = "Revoca el token en el servidor. Con reason=INACTIVITY (timeout del frontend) "
                    + "responde 401 SESSION_EXPIRED tras revocar; con reason=USER (por defecto) responde 204.")
    @ApiResponse(responseCode = "204", description = "Sesión cerrada")
    @ApiResponse(responseCode = "401", description = "SESSION_EXPIRED por inactividad",
            content = @Content(schema = @Schema(implementation = SessionErrorResponse.class)))
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody(required = false) LogoutRequestDTO request) {
        LogoutReason reason = request != null && request.getReason() != null
                ? request.getReason()
                : LogoutReason.USER;
        sessionUseCase.logout(extractToken(authorization), reason);
        if (reason == LogoutReason.INACTIVITY) {
            throw new SessionExpiredException("Inactivity limit reached");
        }
        return ResponseEntity.noContent().build();
    }

    private String extractToken(String authorization) {
        return authorization != null && authorization.startsWith(BEARER_PREFIX)
                ? authorization.substring(BEARER_PREFIX.length())
                : null;
    }
}

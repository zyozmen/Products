package com.zyozmen.products.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta de un login exitoso: token JWT y perfil del usuario")
public class LoginResponseDTO {

    @Schema(description = "Token de sesión JWT")
    private String token;

    @Schema(description = "Perfil del usuario autenticado")
    private UserResponseDTO user;
}

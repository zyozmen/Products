package com.zyozmen.products.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Credenciales de inicio de sesión")
public class LoginRequestDTO {

    @NotBlank(message = "El username es obligatorio")
    @Schema(description = "Nombre de usuario", example = "juan123")
    private String username;

    @NotBlank(message = "El password es obligatorio")
    @Schema(description = "Contraseña", example = "mi_password_seguro")
    private String password;
}

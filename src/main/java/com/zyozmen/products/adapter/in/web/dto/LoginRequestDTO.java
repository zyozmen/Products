package com.zyozmen.products.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El correo electrónico no es válido")
    @Schema(description = "Correo electrónico registrado", example = "juan@example.com")
    private String email;

    @NotBlank(message = "El password es obligatorio")
    @Schema(description = "Contraseña", example = "mi_password_seguro")
    private String password;
}

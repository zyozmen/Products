package com.zyozmen.products.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos de entrada para el registro de un nuevo usuario")
public class RegisterRequestDTO {

    @NotBlank(message = "El username es obligatorio")
    @Schema(description = "Nombre de usuario para iniciar sesión", example = "juan123")
    private String username;

    @NotBlank(message = "El password es obligatorio")
    @Schema(description = "Contraseña del usuario", example = "mi_password_seguro")
    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    @Schema(description = "Nombre del usuario", example = "Juan")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Schema(description = "Apellido del usuario", example = "Pérez")
    private String apellido;

    @NotBlank(message = "La dirección es obligatoria")
    @Schema(description = "Dirección del usuario", example = "Calle 45 # 12-34")
    private String direccion;

    @NotBlank(message = "El teléfono es obligatorio")
    @Schema(description = "Teléfono del usuario", example = "3124567890")
    private String telefono;

    @NotNull(message = "El tipo de identificación es obligatorio")
    @Schema(description = "Tipo de documento de identificación", example = "CC")
    private String tipoIdentificacion;

    @NotBlank(message = "El número de identificación es obligatorio")
    @Schema(description = "Número de documento de identificación", example = "1018234567")
    private String numeroIdentificacion;

    @NotNull(message = "Debe confirmar si es mayor de edad")
    @AssertTrue(message = "Debe ser mayor de 18 años para registrarse")
    @Schema(description = "Confirmación explícita de ser mayor de 18 años", example = "true")
    private Boolean mayorDeEdad;
}

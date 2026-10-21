package com.zyozmen.products.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos de entrada para actualizar el perfil del usuario autenticado.
 * Todos los campos son opcionales: solo se actualizan los que vengan informados.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos de perfil a actualizar (todos los campos son opcionales)")
public class UpdateProfileRequestDTO {

    @Schema(description = "Nombre del usuario", example = "Juan")
    private String nombre;

    @Schema(description = "Apellido del usuario", example = "Pérez")
    private String apellido;

    @Schema(description = "Dirección del usuario", example = "Calle 45 # 12-34")
    private String direccion;

    @Schema(description = "Teléfono del usuario", example = "3124567890")
    private String telefono;

    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    @Schema(description = "Nueva contraseña del usuario (opcional)", example = "mi_password_seguro")
    private String password;
}

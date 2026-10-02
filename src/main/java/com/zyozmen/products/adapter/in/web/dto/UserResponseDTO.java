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
@Schema(description = "Datos de un usuario expuestos al cliente (sin password)")
public class UserResponseDTO {

    @Schema(description = "Nombre de usuario", example = "juan123")
    private String username;

    @Schema(description = "Nombre", example = "Juan")
    private String nombre;

    @Schema(description = "Apellido", example = "Pérez")
    private String apellido;

    @Schema(description = "Dirección", example = "Calle 45 # 12-34")
    private String direccion;

    @Schema(description = "Teléfono", example = "3124567890")
    private String telefono;

    @Schema(description = "Tipo de documento de identificación", example = "CC")
    private String tipoIdentificacion;

    @Schema(description = "Número de documento de identificación", example = "1018234567")
    private String numeroIdentificacion;

    @Schema(description = "Rol del usuario", example = "cliente")
    private String role;

    @Schema(description = "Indica si la cuenta está activa", example = "true")
    private Boolean active;
}

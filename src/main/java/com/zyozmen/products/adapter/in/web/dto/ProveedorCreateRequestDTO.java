package com.zyozmen.products.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProveedorCreateRequestDTO(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 150, message = "El nombre es demasiado largo")
        String nombre,
        @NotBlank(message = "El NIT es obligatorio") @Size(max = 30, message = "El NIT es demasiado largo")
        String nit,
        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "El teléfono no es válido") String telefono,
        @Email(message = "El email no es válido") String email) {
}

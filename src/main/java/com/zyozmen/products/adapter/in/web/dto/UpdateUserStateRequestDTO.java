package com.zyozmen.products.adapter.in.web.dto;

import com.zyozmen.products.domain.model.UserState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cambio de estado de un usuario (solo ADMIN)")
public class UpdateUserStateRequestDTO {

    @NotNull(message = "El estado es obligatorio (ACTIVO, INACTIVO o PENDIENTE)")
    @Schema(example = "ACTIVO", allowableValues = {"ACTIVO", "INACTIVO", "PENDIENTE"})
    private UserState state;
}

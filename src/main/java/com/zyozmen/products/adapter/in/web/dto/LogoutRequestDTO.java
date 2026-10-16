package com.zyozmen.products.adapter.in.web.dto;

import com.zyozmen.products.domain.model.LogoutReason;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Evento de cierre de sesión")
public class LogoutRequestDTO {

    @Schema(description = "Motivo del cierre: USER (por defecto) o INACTIVITY (timeout del frontend)",
            example = "INACTIVITY")
    private LogoutReason reason;
}

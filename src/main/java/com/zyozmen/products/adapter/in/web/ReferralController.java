package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.ReferralPageResponseDTO;
import com.zyozmen.products.domain.port.in.UserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Referidos", description = "Referidos del usuario autenticado")
public class ReferralController {
    private final UserUseCase userUseCase;

    @Operation(summary = "Listar mis referidos",
            description = "Pagina desde 0; size entre 1 y 100. Orden: vinculacion mas reciente primero.")
    @ApiResponse(responseCode = "200", description = "Pagina de datos basicos sin informacion sensible")
    @ApiResponse(responseCode = "400", description = "Paginacion invalida")
    @ApiResponse(responseCode = "401", description = "Autenticacion requerida")
    @GetMapping({"/api/v1/users/profile/referrals", "/api/v1/users/me/referrals"})
    public ReferralPageResponseDTO listar(Authentication authentication,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = userUseCase.listarReferidos(authentication.getName(), page, size);
        var content = result.content().stream().map(item -> new ReferralPageResponseDTO.Item(
                item.username(), item.nombre(), item.apellido(), item.createdAt())).toList();
        long totalPages = result.totalElements() / result.size()
                + (result.totalElements() % result.size() == 0 ? 0 : 1);
        return new ReferralPageResponseDTO(content, result.page(), result.size(),
                result.totalElements(), totalPages);
    }
}

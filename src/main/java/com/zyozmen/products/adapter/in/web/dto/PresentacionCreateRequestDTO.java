package com.zyozmen.products.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.zyozmen.products.domain.port.in.PresentacionUseCase.CrearPresentacionCommand;
import com.zyozmen.products.domain.model.Imagen;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PresentacionCreateRequestDTO(
        @Pattern(regexp = "[0-9a-fA-F]{24}", message = "El presentacionId debe ser un ObjectId valido")
        String presentacionId,
        @NotBlank @Size(max = 100) String sku,
        @NotBlank @Size(max = 200) String nombre,
        @NotNull @Positive @Digits(integer = 15, fraction = 3) BigDecimal contenidoUnidad,
        @NotNull @Min(1) Integer unidadesCreadas,
        @NotNull @DecimalMin("0.00") @Digits(integer = 15, fraction = 2) BigDecimal precioVenta,
        boolean activoMarketplace,
        boolean confirmacionMargenNegativo,
        @Valid FotografiaRequestDTO fotografia) {

    @JsonAnySetter
    public void rechazarCampoDesconocido(String campo, JsonNode valor) {
        throw new IllegalArgumentException("Campo no soportado: " + campo
                + ". Envie la imagen en fotografia con nombre, extension y filepart Base64");
    }

    public record FotografiaRequestDTO(
            @NotBlank @Size(max = 255) String nombre,
            @NotBlank @Pattern(regexp = "(?i:png|jpg|jpeg|webp)") String extension,
            @NotBlank @Size(max = 273068, message = "La fotografia no puede superar 200 KiB")
            String filepart) {
        public Imagen toDomain() {
            return new Imagen(nombre, extension, filepart);
        }
    }

    public CrearPresentacionCommand toCommand() {
        return new CrearPresentacionCommand(presentacionId, sku, nombre, contenidoUnidad,
                unidadesCreadas, precioVenta, activoMarketplace, confirmacionMargenNegativo,
                fotografia == null ? null : fotografia.toDomain());
    }
}

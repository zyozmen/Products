package com.zyozmen.products.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class FotografiaPresentacionTest {
    @Test
    void admiteFormatoDelServicioDeImagenesYNormalizaExtensionYBase64() {
        for (String extension : List.of("png", "jpg", "jpeg", "webp")) {
            var image = FotografiaPresentacion.validar(new Imagen("foto." + extension,
                    extension.toUpperCase(), "AQID"));
            assertThat(image).isEqualTo(new Imagen("foto." + extension, extension, "AQID"));
        }
        assertThat(FotografiaPresentacion.validar(null)).isNull();
        assertThat(FotografiaPresentacion.validar(new Imagen("foto.png", "png", "AQ")).getFilepart())
                .isEqualTo("AQ==");
    }

    @Test
    void aceptaExactamente200KiBYRechazaUnByteMasAntesDePersistir() {
        String limite = Base64.getEncoder().encodeToString(new byte[204800]);
        String excesiva = Base64.getEncoder().encodeToString(new byte[204801]);
        assertThat(FotografiaPresentacion.validar(new Imagen("foto.png", "png", limite)).getFilepart())
                .isEqualTo(limite);
        assertThatThrownBy(() -> FotografiaPresentacion.validar(new Imagen("foto.png", "png", excesiva)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("200 KiB");
        String mayor = Base64.getEncoder().encodeToString(new byte[204804]);
        assertThatThrownBy(() -> FotografiaPresentacion.validar(new Imagen("foto.png", "png", mayor)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("200 KiB");
    }

    @Test
    void rechazaUrlDataUrlContenidoVacioYBase64Invalido() {
        for (String contenido : List.of("", "https://example.com/image.png", "data:image/png;base64,AQID", "%%%")) {
            assertThatThrownBy(() -> FotografiaPresentacion.validar(new Imagen("foto.png", "png", contenido)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> FotografiaPresentacion.validar(new Imagen("foto.png", "png", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaMetadataInvalidaYNoReinterpretaFormatos() {
        for (Imagen imagen : List.of(new Imagen(null, "png", "AQID"),
                new Imagen(" ", "png", "AQID"), new Imagen("foto.png", null, "AQID"),
                new Imagen("foto.svg", "svg", "AQID"), new Imagen("foto.jpg", "png", "AQID"),
                new Imagen("foto", "png", "AQID"),
                new Imagen("a".repeat(256) + ".png", "png", "AQID"))) {
            assertThatThrownBy(() -> FotografiaPresentacion.validar(imagen)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}

package com.zyozmen.products.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class FotografiaPresentacionTest {
    @Test
    void admiteFormatoDelServicioDeImagenesYNormalizaExtensionYBase64() {
        for (String extension : List.of("png", "jpg", "jpeg", "webp")) {
            var image = PresentationPicture.validar(new Image("foto." + extension,
                    extension.toUpperCase(), "AQID"));
            assertThat(image).isEqualTo(new Image("foto." + extension, extension, "AQID"));
        }
        assertThat(PresentationPicture.validar(null)).isNull();
        assertThat(PresentationPicture.validar(new Image("foto.png", "png", "AQ")).getFilepart())
                .isEqualTo("AQ==");
    }

    @Test
    void aceptaExactamente200KiBYRechazaUnByteMasAntesDePersistir() {
        String limite = Base64.getEncoder().encodeToString(new byte[204800]);
        String excesiva = Base64.getEncoder().encodeToString(new byte[204801]);
        assertThat(PresentationPicture.validar(new Image("foto.png", "png", limite)).getFilepart())
                .isEqualTo(limite);
        assertThatThrownBy(() -> PresentationPicture.validar(new Image("foto.png", "png", excesiva)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("200 KiB");
        String mayor = Base64.getEncoder().encodeToString(new byte[204804]);
        assertThatThrownBy(() -> PresentationPicture.validar(new Image("foto.png", "png", mayor)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("200 KiB");
    }

    @Test
    void rechazaUrlDataUrlContenidoVacioYBase64Invalido() {
        for (String contenido : List.of("", "https://example.com/image.png", "data:image/png;base64,AQID", "%%%")) {
            assertThatThrownBy(() -> PresentationPicture.validar(new Image("foto.png", "png", contenido)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> PresentationPicture.validar(new Image("foto.png", "png", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaMetadataInvalidaYNoReinterpretaFormatos() {
        for (Image imagen : List.of(new Image(null, "png", "AQID"),
                new Image(" ", "png", "AQID"), new Image("foto.png", null, "AQID"),
                new Image("foto.svg", "svg", "AQID"), new Image("foto.jpg", "png", "AQID"),
                new Image("foto", "png", "AQID"),
                new Image("a".repeat(256) + ".png", "png", "AQID"))) {
            assertThatThrownBy(() -> PresentationPicture.validar(imagen)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}

package com.zyozmen.products.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Contenedor de las imágenes de un producto (principal y secundarias).
 */
public class Images {

    private Image fotoPrincipal;

    public Images() {
    }

    public Images(Image fotoPrincipal) {
        this.fotoPrincipal = fotoPrincipal;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Image getFotoPrincipal() {
        return fotoPrincipal;
    }

    public void setFotoPrincipal(Image fotoPrincipal) {
        this.fotoPrincipal = fotoPrincipal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Images images = (Images) o;
        return Objects.equals(fotoPrincipal, images.fotoPrincipal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fotoPrincipal);
    }

    @Override
    public String toString() {
        return "Images{" +
                "fotoPrincipal=" + fotoPrincipal +
                '}';
    }

    public static class Builder {
        private Image fotoPrincipal;
        public Builder fotoPrincipal(Image fotoPrincipal) {
            this.fotoPrincipal = fotoPrincipal;
            return this;
        }

        public Images build() {
            return new Images(fotoPrincipal);
        }
    }
}

package com.zyozmen.products.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Contenedor de las imágenes de un producto (principal y secundarias).
 */
public class Images {

    private Imagen fotoPrincipal;
    private List<Imagen> fotosSecundarias;

    public Images() {
    }

    public Images(Imagen fotoPrincipal, List<Imagen> fotosSecundarias) {
        this.fotoPrincipal = fotoPrincipal;
        this.fotosSecundarias = fotosSecundarias;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Imagen getFotoPrincipal() {
        return fotoPrincipal;
    }

    public void setFotoPrincipal(Imagen fotoPrincipal) {
        this.fotoPrincipal = fotoPrincipal;
    }

    public List<Imagen> getFotosSecundarias() {
        return fotosSecundarias;
    }

    public void setFotosSecundarias(List<Imagen> fotosSecundarias) {
        this.fotosSecundarias = fotosSecundarias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Images images = (Images) o;
        return Objects.equals(fotoPrincipal, images.fotoPrincipal)
                && Objects.equals(fotosSecundarias, images.fotosSecundarias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fotoPrincipal, fotosSecundarias);
    }

    @Override
    public String toString() {
        return "Images{" +
                "fotoPrincipal=" + fotoPrincipal +
                ", fotosSecundarias=" + fotosSecundarias +
                '}';
    }

    public static class Builder {
        private Imagen fotoPrincipal;
        private List<Imagen> fotosSecundarias;

        public Builder fotoPrincipal(Imagen fotoPrincipal) {
            this.fotoPrincipal = fotoPrincipal;
            return this;
        }

        public Builder fotosSecundarias(List<Imagen> fotosSecundarias) {
            this.fotosSecundarias = fotosSecundarias;
            return this;
        }

        public Images build() {
            return new Images(fotoPrincipal, fotosSecundarias);
        }
    }
}

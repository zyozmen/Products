package com.zyozmen.products.domain.model;

import java.util.Objects;

/**
 * Representa una imagen con sus metadatos y contenido Base64.
 */
public class Imagen {

    private String nombre;
    private String extension;
    private String filepart;

    public Imagen() {
    }

    public Imagen(String nombre, String extension, String filepart) {
        this.nombre = nombre;
        this.extension = extension;
        this.filepart = filepart;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getFilepart() {
        return filepart;
    }

    public void setFilepart(String filepart) {
        this.filepart = filepart;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Imagen imagen = (Imagen) o;
        return Objects.equals(nombre, imagen.nombre)
                && Objects.equals(extension, imagen.extension)
                && Objects.equals(filepart, imagen.filepart);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre, extension, filepart);
    }

    @Override
    public String toString() {
        return "Imagen{" +
                "nombre='" + nombre + '\'' +
                ", extension='" + extension + '\'' +
                ", filepart='" + filepart + '\'' +
                '}';
    }

    public static class Builder {
        private String nombre;
        private String extension;
        private String filepart;

        public Builder nombre(String nombre) {
            this.nombre = nombre;
            return this;
        }

        public Builder extension(String extension) {
            this.extension = extension;
            return this;
        }

        public Builder filepart(String filepart) {
            this.filepart = filepart;
            return this;
        }

        public Imagen build() {
            return new Imagen(nombre, extension, filepart);
        }
    }
}

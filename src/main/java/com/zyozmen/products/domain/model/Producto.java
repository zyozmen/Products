package com.zyozmen.products.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Modelo de dominio Producto. 
 * 
 */
public class Producto {

    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;

    public Producto() {
    }

    public Producto(Long id, String nombre, String descripcion, BigDecimal precio) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Producto producto = (Producto) o;
        return Objects.equals(id, producto.id)
                && Objects.equals(nombre, producto.nombre)
                && Objects.equals(descripcion, producto.descripcion)
                && Objects.equals(precio, producto.precio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nombre, descripcion, precio);
    }

    @Override
    public String toString() {
        return "Producto{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", precio=" + precio +
                '}';
    }

    public static class Builder {

        private Long id;
        private String nombre;
        private String descripcion;
        private BigDecimal precio;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder nombre(String nombre) {
            this.nombre = nombre;
            return this;
        }

        public Builder descripcion(String descripcion) {
            this.descripcion = descripcion;
            return this;
        }

        public Builder precio(BigDecimal precio) {
            this.precio = precio;
            return this;
        }

        public Producto build() {
            return new Producto(id, nombre, descripcion, precio);
        }
    }
}

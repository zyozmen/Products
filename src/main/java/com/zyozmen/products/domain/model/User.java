package com.zyozmen.products.domain.model;

import java.util.Objects;

/**
 * Modelo de dominio Usuario.
 *
 * Representa a un usuario del sistema (cliente o administrador). El
 * password almacenado aquí ya se asume hasheado (bcrypt o similar);
 * el dominio nunca trabaja con contraseñas en texto plano una vez
 * persistidas.
 */
public class User {

    private String id;
    private String username;
    private String password;
    private String nombre;
    private String apellido;
    private String direccion;
    private String telefono;
    private TipoIdentificacion tipoIdentificacion;
    private String numeroIdentificacion;
    private Boolean mayorDeEdad;
    private Boolean active;
    private Role role;

    public User() {
    }

    private User(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.password = builder.password;
        this.nombre = builder.nombre;
        this.apellido = builder.apellido;
        this.direccion = builder.direccion;
        this.telefono = builder.telefono;
        this.tipoIdentificacion = builder.tipoIdentificacion;
        this.numeroIdentificacion = builder.numeroIdentificacion;
        this.mayorDeEdad = builder.mayorDeEdad;
        this.active = builder.active;
        this.role = builder.role;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public TipoIdentificacion getTipoIdentificacion() {
        return tipoIdentificacion;
    }

    public void setTipoIdentificacion(TipoIdentificacion tipoIdentificacion) {
        this.tipoIdentificacion = tipoIdentificacion;
    }

    public String getNumeroIdentificacion() {
        return numeroIdentificacion;
    }

    public void setNumeroIdentificacion(String numeroIdentificacion) {
        this.numeroIdentificacion = numeroIdentificacion;
    }

    public Boolean getMayorDeEdad() {
        return mayorDeEdad;
    }

    public void setMayorDeEdad(Boolean mayorDeEdad) {
        this.mayorDeEdad = mayorDeEdad;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id)
                && Objects.equals(username, user.username)
                && Objects.equals(password, user.password)
                && Objects.equals(nombre, user.nombre)
                && Objects.equals(apellido, user.apellido)
                && Objects.equals(direccion, user.direccion)
                && Objects.equals(telefono, user.telefono)
                && tipoIdentificacion == user.tipoIdentificacion
                && Objects.equals(numeroIdentificacion, user.numeroIdentificacion)
                && Objects.equals(mayorDeEdad, user.mayorDeEdad)
                && Objects.equals(active, user.active)
                && role == user.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, password, nombre, apellido, direccion, telefono,
                tipoIdentificacion, numeroIdentificacion, mayorDeEdad, active, role);
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", nombre='" + nombre + '\'' +
                ", apellido='" + apellido + '\'' +
                ", direccion='" + direccion + '\'' +
                ", telefono='" + telefono + '\'' +
                ", tipoIdentificacion=" + tipoIdentificacion +
                ", numeroIdentificacion='" + numeroIdentificacion + '\'' +
                ", mayorDeEdad=" + mayorDeEdad +
                ", active=" + active +
                ", role=" + role +
                '}';
    }

    public static class Builder {

        private String id;
        private String username;
        private String password;
        private String nombre;
        private String apellido;
        private String direccion;
        private String telefono;
        private TipoIdentificacion tipoIdentificacion;
        private String numeroIdentificacion;
        private Boolean mayorDeEdad;
        private Boolean active;
        private Role role;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder nombre(String nombre) {
            this.nombre = nombre;
            return this;
        }

        public Builder apellido(String apellido) {
            this.apellido = apellido;
            return this;
        }

        public Builder direccion(String direccion) {
            this.direccion = direccion;
            return this;
        }

        public Builder telefono(String telefono) {
            this.telefono = telefono;
            return this;
        }

        public Builder tipoIdentificacion(TipoIdentificacion tipoIdentificacion) {
            this.tipoIdentificacion = tipoIdentificacion;
            return this;
        }

        public Builder numeroIdentificacion(String numeroIdentificacion) {
            this.numeroIdentificacion = numeroIdentificacion;
            return this;
        }

        public Builder mayorDeEdad(Boolean mayorDeEdad) {
            this.mayorDeEdad = mayorDeEdad;
            return this;
        }

        public Builder active(Boolean active) {
            this.active = active;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}

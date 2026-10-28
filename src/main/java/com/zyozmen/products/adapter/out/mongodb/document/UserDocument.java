package com.zyozmen.products.adapter.out.mongodb.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Documento MongoDB de infraestructura para la colección de usuarios.
 * El dominio no conoce esta clase ni sus anotaciones de persistencia.
 */
@Document(collection = "Users")
@CompoundIndex(name = "tipo_numero_identificacion_unique",
        def = "{'tipo_identificacion': 1, 'numero_identificacion': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("username")
    private String username;

    @Field("password")
    private String password;

    @Field("nombre")
    private String nombre;

    @Field("apellido")
    private String apellido;

    @Field("direccion")
    private String direccion;

    @Field("telefono")
    private String telefono;

    @Indexed(unique = true, sparse = true)
    @Field("email")
    private String email;

    @Field("tipo_identificacion")
    private String tipoIdentificacion;

    @Field("numero_identificacion")
    private String numeroIdentificacion;

    @Field("mayor_de_edad")
    private Boolean mayorDeEdad;

    @Field("state")
    private String state;

    // Campo heredado (antes "active"): solo lectura para migrar documentos antiguos.
    @Field("active")
    private Boolean active;

    @Field("role")
    private String role;
}

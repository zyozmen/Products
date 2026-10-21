package com.zyozmen.products.adapter.out.mongodb.mapper;

import com.zyozmen.products.adapter.out.mongodb.document.UserMongoDocument;
import com.zyozmen.products.domain.model.Role;
import com.zyozmen.products.domain.model.TipoIdentificacion;
import com.zyozmen.products.domain.model.User;
import com.zyozmen.products.domain.model.UserState;
import org.springframework.stereotype.Component;

/**
 * Mapper entre el documento MongoDB de usuario y el modelo de dominio User.
 */
@Component
public class UserMongoMapper {

    public User toDomain(UserMongoDocument document) {
        if (document == null) {
            return null;
        }
        return User.builder()
                .id(document.getId())
                .username(document.getUsername())
                .password(document.getPassword())
                .nombre(document.getNombre())
                .apellido(document.getApellido())
                .direccion(document.getDireccion())
                .telefono(document.getTelefono())
                .email(document.getEmail())
                .tipoIdentificacion(document.getTipoIdentificacion() != null
                        ? TipoIdentificacion.valueOf(document.getTipoIdentificacion())
                        : null)
                .numeroIdentificacion(document.getNumeroIdentificacion())
                .mayorDeEdad(document.getMayorDeEdad())
                .state(resolveState(document))
                .role(document.getRole() != null ? Role.valueOf(document.getRole()) : null)
                .build();
    }

    public UserMongoDocument toDocument(User user) {
        return toDocument(user, null);
    }

    public UserMongoDocument toDocument(User user, String existingId) {
        if (user == null) {
            return null;
        }
        return UserMongoDocument.builder()
                .id(existingId != null ? existingId : user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .nombre(user.getNombre())
                .apellido(user.getApellido())
                .direccion(user.getDireccion())
                .telefono(user.getTelefono())
                .email(user.getEmail())
                .tipoIdentificacion(user.getTipoIdentificacion() != null
                        ? user.getTipoIdentificacion().name()
                        : null)
                .numeroIdentificacion(user.getNumeroIdentificacion())
                .mayorDeEdad(user.getMayorDeEdad())
                .state(user.getState() != null ? user.getState().name() : null)
                .role(user.getRole() != null ? user.getRole().name() : null)
                .build();
    }

    private UserState resolveState(UserMongoDocument document) {
        if (document.getState() != null) {
            return UserState.valueOf(document.getState());
        }
        if (document.getActive() != null) {
            return document.getActive() ? UserState.ACTIVO : UserState.INACTIVO;
        }
        return null;
    }
}

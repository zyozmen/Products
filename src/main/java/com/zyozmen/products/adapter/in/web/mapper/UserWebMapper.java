package com.zyozmen.products.adapter.in.web.mapper;

import com.zyozmen.products.adapter.in.web.dto.LoginResponseDTO;
import com.zyozmen.products.adapter.in.web.dto.RegisterRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.UserResponseDTO;
import com.zyozmen.products.domain.model.AuthResult;
import com.zyozmen.products.domain.model.TipoIdentificacion;
import com.zyozmen.products.domain.model.User;
import org.springframework.stereotype.Component;

/**
 * Mapper entre los DTOs web de usuario/autenticación y el modelo de dominio User.
 */
@Component
public class UserWebMapper {

    public User toDomain(RegisterRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        return User.builder()
                .username(dto.getUsername())
                .password(dto.getPassword())
                .nombre(dto.getNombre())
                .apellido(dto.getApellido())
                .direccion(dto.getDireccion())
                .telefono(dto.getTelefono())
                .tipoIdentificacion(dto.getTipoIdentificacion() != null
                        ? TipoIdentificacion.valueOf(dto.getTipoIdentificacion())
                        : null)
                .numeroIdentificacion(dto.getNumeroIdentificacion())
                .mayorDeEdad(dto.getMayorDeEdad())
                .build();
    }

    public UserResponseDTO toResponseDTO(User user) {
        if (user == null) {
            return null;
        }
        return UserResponseDTO.builder()
                .username(user.getUsername())
                .nombre(user.getNombre())
                .apellido(user.getApellido())
                .direccion(user.getDireccion())
                .telefono(user.getTelefono())
                .tipoIdentificacion(user.getTipoIdentificacion() != null
                        ? user.getTipoIdentificacion().name()
                        : null)
                .numeroIdentificacion(user.getNumeroIdentificacion())
                .role(user.getRole() != null ? user.getRole().name().toLowerCase() : null)
                .active(user.getActive())
                .build();
    }

    public LoginResponseDTO toLoginResponseDTO(AuthResult authResult) {
        if (authResult == null) {
            return null;
        }
        return LoginResponseDTO.builder()
                .token(authResult.getToken())
                .user(toResponseDTO(authResult.getUser()))
                .build();
    }
}

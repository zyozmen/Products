package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.UpdateProfileRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.UserResponseDTO;
import com.zyozmen.products.adapter.in.web.mapper.UserWebMapper;
import com.zyozmen.products.domain.model.User;
import com.zyozmen.products.domain.port.in.UserUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

    @Mock
    private UserUseCase userUseCase;

    @Mock
    private UserWebMapper userWebMapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserProfileController userProfileController;

    @Test
    void obtenerPerfilShouldReturnAuthenticatedUserProfile() {
        User user = User.builder().username("juan123").build();
        UserResponseDTO dto = UserResponseDTO.builder().username("juan123").build();

        when(authentication.getName()).thenReturn("juan123");
        when(userUseCase.obtenerPorUsername("juan123")).thenReturn(user);
        when(userWebMapper.toResponseDTO(user)).thenReturn(dto);

        ResponseEntity<UserResponseDTO> response = userProfileController.obtenerPerfil(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void actualizarPerfilShouldDelegateToUseCaseWithAuthenticatedUsername() {
        UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .nombre("Juan Carlos")
                .apellido("Pérez")
                .direccion("Nueva dirección")
                .telefono("3000000000")
                .password("nueva-clave")
                .build();

        User updatedUser = User.builder().username("juan123").nombre("Juan Carlos").build();
        UserResponseDTO dto = UserResponseDTO.builder().username("juan123").nombre("Juan Carlos").build();

        when(authentication.getName()).thenReturn("juan123");
        when(userUseCase.actualizarPerfil(
                "juan123", "Juan Carlos", "Pérez", "Nueva dirección", "3000000000", "nueva-clave"))
                .thenReturn(updatedUser);
        when(userWebMapper.toResponseDTO(updatedUser)).thenReturn(dto);

        ResponseEntity<UserResponseDTO> response =
                userProfileController.actualizarPerfil(authentication, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }
}

package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.UpdateUserStateRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.UserResponseDTO;
import com.zyozmen.products.adapter.in.web.mapper.UserWebMapper;
import com.zyozmen.products.domain.model.User;
import com.zyozmen.products.domain.model.UserState;
import com.zyozmen.products.domain.port.in.UserUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserUseCase userUseCase;

    @Mock
    private UserWebMapper userWebMapper;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    void listarUsuariosShouldReturnMappedUserList() {
        User user = User.builder().username("juan123").build();
        UserResponseDTO dto = UserResponseDTO.builder().username("juan123").build();

        when(userUseCase.listarUsuarios()).thenReturn(List.of(user));
        when(userWebMapper.toResponseDTO(user)).thenReturn(dto);

        ResponseEntity<List<UserResponseDTO>> response = adminUserController.listarUsuarios();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(dto);
    }

    @Test
    void cambiarEstadoShouldReturnUpdatedUser() {
        User updated = User.builder().username("juan123").state(UserState.ACTIVO).build();
        UserResponseDTO dto = UserResponseDTO.builder().username("juan123").state("ACTIVO").build();

        when(userUseCase.cambiarEstado("juan123", UserState.ACTIVO)).thenReturn(updated);
        when(userWebMapper.toResponseDTO(updated)).thenReturn(dto);

        ResponseEntity<UserResponseDTO> response = adminUserController.cambiarEstado(
                "juan123", UpdateUserStateRequestDTO.builder().state(UserState.ACTIVO).build());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(dto);
    }
}

package com.zyozmen.products.adapter.in.web;

import com.zyozmen.products.adapter.in.web.dto.LoginRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.LoginResponseDTO;
import com.zyozmen.products.adapter.in.web.dto.RegisterRequestDTO;
import com.zyozmen.products.adapter.in.web.dto.UserResponseDTO;
import com.zyozmen.products.adapter.in.web.mapper.UserWebMapper;
import com.zyozmen.products.domain.model.AuthResult;
import com.zyozmen.products.domain.model.Role;
import com.zyozmen.products.domain.model.User;
import com.zyozmen.products.domain.port.in.UserUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserUseCase userUseCase;

    @Mock
    private UserWebMapper userWebMapper;

    @InjectMocks
    private AuthController authController;

    @Test
    void registerShouldReturn201WithCreatedUser() {
        RegisterRequestDTO request = RegisterRequestDTO.builder()
                .username("juan123")
                .password("secret")
                .nombre("Juan")
                .apellido("Pérez")
                .direccion("Calle 45 # 12-34")
                .telefono("3124567890")
                .tipoIdentificacion("CC")
                .numeroIdentificacion("1018234567")
                .mayorDeEdad(true)
                .build();

        User domainRequest = User.builder().username("juan123").build();
        User savedUser = User.builder().username("juan123").role(Role.CLIENTE).active(true).build();
        UserResponseDTO responseDTO = UserResponseDTO.builder().username("juan123").role("cliente").build();

        when(userWebMapper.toDomain(request)).thenReturn(domainRequest);
        when(userUseCase.registrar(domainRequest)).thenReturn(savedUser);
        when(userWebMapper.toResponseDTO(savedUser)).thenReturn(responseDTO);

        ResponseEntity<UserResponseDTO> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(responseDTO);
    }

    @Test
    void loginShouldReturn200WithTokenAndUser() {
        LoginRequestDTO request = LoginRequestDTO.builder().username("juan123").password("secret").build();
        User user = User.builder().username("juan123").build();
        AuthResult authResult = new AuthResult("jwt-token", user);
        LoginResponseDTO responseDTO = LoginResponseDTO.builder().token("jwt-token").build();

        when(userUseCase.login("juan123", "secret")).thenReturn(authResult);
        when(userWebMapper.toLoginResponseDTO(authResult)).thenReturn(responseDTO);

        ResponseEntity<LoginResponseDTO> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(responseDTO);
    }
}

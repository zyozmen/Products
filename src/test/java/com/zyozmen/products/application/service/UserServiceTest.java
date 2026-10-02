package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.InvalidCredentialsException;
import com.zyozmen.products.domain.exception.MainAdminProtectedException;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.exception.UnderageRegistrationException;
import com.zyozmen.products.domain.exception.UserAlreadyExistsException;
import com.zyozmen.products.domain.exception.UserInactiveException;
import com.zyozmen.products.domain.model.AuthResult;
import com.zyozmen.products.domain.model.Role;
import com.zyozmen.products.domain.model.TipoIdentificacion;
import com.zyozmen.products.domain.model.User;
import com.zyozmen.products.domain.port.out.UserRepositoryPort;
import com.zyozmen.products.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "mainAdminUsername", "admin");
    }

    private User buildClienteRegistration() {
        return User.builder()
                .username("juan123")
                .password("raw-password")
                .nombre("Juan")
                .apellido("Pérez")
                .direccion("Calle 45 # 12-34")
                .telefono("3124567890")
                .tipoIdentificacion(TipoIdentificacion.CC)
                .numeroIdentificacion("1018234567")
                .mayorDeEdad(true)
                .build();
    }

    @Test
    void registrarShouldHashPasswordAndForceRoleClienteAndActive() {
        User request = buildClienteRegistration();
        when(userRepositoryPort.existsByUsername("juan123")).thenReturn(false);
        when(userRepositoryPort.existsByTipoIdentificacionAndNumeroIdentificacion(
                TipoIdentificacion.CC, "1018234567")).thenReturn(false);
        when(passwordEncoder.encode("raw-password")).thenReturn("hashed-password");
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.registrar(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(captor.capture());

        assertThat(captor.getValue().getPassword()).isEqualTo("hashed-password");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.CLIENTE);
        assertThat(captor.getValue().getActive()).isTrue();
        assertThat(result.getRole()).isEqualTo(Role.CLIENTE);
    }

    @Test
    void registrarShouldThrowWhenUsernameAlreadyExists() {
        User request = buildClienteRegistration();
        when(userRepositoryPort.existsByUsername("juan123")).thenReturn(true);

        assertThatThrownBy(() -> userService.registrar(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("nombre de usuario ya está registrado");

        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    void registrarShouldThrowWhenIdentificationAlreadyExists() {
        User request = buildClienteRegistration();
        when(userRepositoryPort.existsByUsername("juan123")).thenReturn(false);
        when(userRepositoryPort.existsByTipoIdentificacionAndNumeroIdentificacion(
                TipoIdentificacion.CC, "1018234567")).thenReturn(true);

        assertThatThrownBy(() -> userService.registrar(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("tipo y número de identificación");

        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    void registrarShouldThrowWhenNotMayorDeEdad() {
        User request = buildClienteRegistration();
        request.setMayorDeEdad(false);
        when(userRepositoryPort.existsByUsername("juan123")).thenReturn(false);
        when(userRepositoryPort.existsByTipoIdentificacionAndNumeroIdentificacion(
                TipoIdentificacion.CC, "1018234567")).thenReturn(false);

        assertThatThrownBy(() -> userService.registrar(request))
                .isInstanceOf(UnderageRegistrationException.class);

        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    void loginShouldReturnTokenAndUserWhenCredentialsValidAndActive() {
        User storedUser = User.builder()
                .username("juan123")
                .password("hashed-password")
                .role(Role.CLIENTE)
                .active(true)
                .build();

        when(userRepositoryPort.findByUsername("juan123")).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches("raw-password", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken("juan123", "CLIENTE")).thenReturn("jwt-token");

        AuthResult result = userService.login("juan123", "raw-password");

        assertThat(result.getToken()).isEqualTo("jwt-token");
        assertThat(result.getUser()).isEqualTo(storedUser);
    }

    @Test
    void loginShouldThrowInvalidCredentialsWhenUserNotFound() {
        when(userRepositoryPort.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login("ghost", "whatever"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginShouldThrowInvalidCredentialsWhenPasswordDoesNotMatch() {
        User storedUser = User.builder()
                .username("juan123")
                .password("hashed-password")
                .active(true)
                .build();

        when(userRepositoryPort.findByUsername("juan123")).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> userService.login("juan123", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginShouldThrowUserInactiveWhenAccountIsInactive() {
        User storedUser = User.builder()
                .username("juan123")
                .password("hashed-password")
                .active(false)
                .build();

        when(userRepositoryPort.findByUsername("juan123")).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches("raw-password", "hashed-password")).thenReturn(true);

        assertThatThrownBy(() -> userService.login("juan123", "raw-password"))
                .isInstanceOf(UserInactiveException.class);
    }

    @Test
    void listarUsuariosShouldDelegateToRepository() {
        User user = User.builder().username("juan123").build();
        when(userRepositoryPort.findAll()).thenReturn(List.of(user));

        List<User> result = userService.listarUsuarios();

        assertThat(result).containsExactly(user);
    }

    @Test
    void toggleEstadoShouldThrowWhenTargetingMainAdmin() {
        assertThatThrownBy(() -> userService.toggleEstado("admin"))
                .isInstanceOf(MainAdminProtectedException.class);
    }

    @Test
    void toggleEstadoShouldThrowWhenUserNotFound() {
        when(userRepositoryPort.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.toggleEstado("ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void toggleEstadoShouldFlipActiveFromTrueToFalse() {
        User storedUser = User.builder().username("juan123").active(true).build();
        when(userRepositoryPort.findByUsername("juan123")).thenReturn(Optional.of(storedUser));
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.toggleEstado("juan123");

        assertThat(result.getActive()).isFalse();
    }

    @Test
    void toggleEstadoShouldFlipActiveFromFalseToTrue() {
        User storedUser = User.builder().username("juan123").active(false).build();
        when(userRepositoryPort.findByUsername("juan123")).thenReturn(Optional.of(storedUser));
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.toggleEstado("juan123");

        assertThat(result.getActive()).isTrue();
    }
}

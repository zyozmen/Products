package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.exception.InvalidCredentialsException;
import com.zyozmen.products.domain.exception.MainAdminProtectedException;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.exception.UnderageRegistrationException;
import com.zyozmen.products.domain.exception.UserAlreadyExistsException;
import com.zyozmen.products.domain.exception.UserInactiveException;
import com.zyozmen.products.domain.model.AuthResult;
import com.zyozmen.products.domain.model.Role;
import com.zyozmen.products.domain.model.IdentificationType;
import com.zyozmen.products.domain.model.User;
import com.zyozmen.products.domain.model.UserState;
import com.zyozmen.products.domain.exception.UserPendingApprovalException;
import com.zyozmen.products.domain.port.out.UserNotificationPort;
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

    @Mock
    private UserNotificationPort userNotificationPort;

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
                .telefono("3124567890").email("Juan@Example.com")
                .tipoIdentificacion(IdentificationType.CC)
                .numeroIdentificacion("1018234567")
                .mayorDeEdad(true)
                .build();
    }

    @Test
    void registrarShouldHashPasswordAndForceRoleClienteAndStatePendiente() {
        User request = buildClienteRegistration();
        when(userRepositoryPort.existsByUsername("juan123")).thenReturn(false);
        when(userRepositoryPort.existsByTipoIdentificacionAndNumeroIdentificacion(
                IdentificationType.CC, "1018234567")).thenReturn(false);
        when(passwordEncoder.encode("raw-password")).thenReturn("hashed-password");
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.registrar(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(captor.capture());

        assertThat(captor.getValue().getPassword()).isEqualTo("hashed-password");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.CLIENTE);
        assertThat(captor.getValue().getState()).isEqualTo(UserState.PENDIENTE);
        assertThat(result.getRole()).isEqualTo(Role.CLIENTE);
        verify(userNotificationPort).sendWelcomeEmail(result);
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
    void registrarShouldThrowWhenEmailAlreadyExists() {
        User request = buildClienteRegistration();
        when(userRepositoryPort.existsByUsername("juan123")).thenReturn(false);
        when(userRepositoryPort.existsByEmail("juan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registrar(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("correo electrónico");

        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    void registrarShouldThrowUserInactiveWhenEmailBelongsToInactiveAccount() {
        User request = buildClienteRegistration();
        when(userRepositoryPort.existsByUsername("juan123")).thenReturn(false);
        when(userRepositoryPort.existsByEmail("juan@example.com")).thenReturn(true);
        when(userRepositoryPort.findByEmail("juan@example.com"))
                .thenReturn(Optional.of(User.builder().state(UserState.INACTIVO).build()));

        assertThatThrownBy(() -> userService.registrar(request))
                .isInstanceOf(UserInactiveException.class)
                .hasMessageContaining("cuenta inactiva");

        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    void registrarShouldThrowWhenIdentificationAlreadyExists() {
        User request = buildClienteRegistration();
        when(userRepositoryPort.existsByUsername("juan123")).thenReturn(false);
        when(userRepositoryPort.existsByTipoIdentificacionAndNumeroIdentificacion(
                IdentificationType.CC, "1018234567")).thenReturn(true);

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
                IdentificationType.CC, "1018234567")).thenReturn(false);

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
                .state(UserState.ACTIVO)
                .build();

        when(userRepositoryPort.findByEmail("juan@example.com")).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches("raw-password", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken("juan123", "CLIENTE")).thenReturn("jwt-token");

        AuthResult result = userService.login("juan@example.com", "raw-password");

        assertThat(result.getToken()).isEqualTo("jwt-token");
        assertThat(result.getUser()).isEqualTo(storedUser);
    }

    @Test
    void loginShouldThrowInvalidCredentialsWhenUserNotFound() {
        when(userRepositoryPort.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login("ghost@example.com", "whatever"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginShouldThrowInvalidCredentialsWhenPasswordDoesNotMatch() {
        User storedUser = User.builder()
                .username("juan123")
                .password("hashed-password")
                .state(UserState.ACTIVO)
                .build();

        when(userRepositoryPort.findByEmail("juan@example.com")).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> userService.login("juan@example.com", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginShouldThrowUserInactiveWhenAccountIsInactive() {
        User storedUser = User.builder()
                .username("juan123")
                .password("hashed-password")
                .state(UserState.INACTIVO)
                .build();

        when(userRepositoryPort.findByEmail("juan@example.com")).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches("raw-password", "hashed-password")).thenReturn(true);

        assertThatThrownBy(() -> userService.login("juan@example.com", "raw-password"))
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
    void cambiarEstadoShouldThrowWhenTargetingMainAdmin() {
        assertThatThrownBy(() -> userService.cambiarEstado("admin", UserState.INACTIVO))
                .isInstanceOf(MainAdminProtectedException.class);
    }

    @Test
    void cambiarEstadoShouldThrowWhenStateIsNull() {
        assertThatThrownBy(() -> userService.cambiarEstado("juan123", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void loginShouldThrowPendingApprovalWhenAccountIsPendiente() {
        User storedUser = User.builder()
                .username("juan123")
                .password("hashed-password")
                .state(UserState.PENDIENTE)
                .build();
        when(userRepositoryPort.findByEmail("juan@example.com")).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches("raw-password", "hashed-password")).thenReturn(true);

        assertThatThrownBy(() -> userService.login("juan@example.com", "raw-password"))
                .isInstanceOf(UserPendingApprovalException.class)
                .hasMessageContaining("pendiente de aprobación");
    }

    @Test
    void cambiarEstadoShouldThrowWhenUserNotFound() {
        when(userRepositoryPort.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.cambiarEstado("ghost", UserState.ACTIVO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cambiarEstadoShouldApproveUserFromPendienteToActivo() {
        User storedUser = User.builder().username("juan123").state(UserState.PENDIENTE).build();
        when(userRepositoryPort.findByUsername("juan123")).thenReturn(Optional.of(storedUser));
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.cambiarEstado("juan123", UserState.ACTIVO);

        assertThat(result.getState()).isEqualTo(UserState.ACTIVO);
    }

    @Test
    void cambiarEstadoShouldAllowInactivatingUser() {
        User storedUser = User.builder().username("juan123").state(UserState.ACTIVO).build();
        when(userRepositoryPort.findByUsername("juan123")).thenReturn(Optional.of(storedUser));
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(userService.cambiarEstado("juan123", UserState.INACTIVO).getState())
                .isEqualTo(UserState.INACTIVO);
    }

    @Test
    void obtenerPorUsernameShouldReturnUserWhenFound() {
        User storedUser = User.builder().username("juan123").build();
        when(userRepositoryPort.findByUsername("juan123")).thenReturn(Optional.of(storedUser));

        User result = userService.obtenerPorUsername("juan123");

        assertThat(result).isEqualTo(storedUser);
    }

    @Test
    void obtenerPorUsernameShouldThrowWhenNotFound() {
        when(userRepositoryPort.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.obtenerPorUsername("ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizarPerfilShouldThrowWhenUserNotFound() {
        when(userRepositoryPort.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.actualizarPerfil(
                "ghost", "Nombre", "Apellido", "Direccion", "Telefono", "nueva-clave"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    void actualizarPerfilShouldUpdateOnlyInformedFields() {
        User storedUser = User.builder()
                .username("juan123")
                .nombre("Juan")
                .apellido("Pérez")
                .direccion("Dirección vieja")
                .telefono("3000000000")
                .password("hashed-old")
                .build();

        when(userRepositoryPort.findByUsername("juan123")).thenReturn(Optional.of(storedUser));
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.actualizarPerfil(
                "juan123", "Juan Carlos", null, "Dirección nueva", null, null);

        assertThat(result.getNombre()).isEqualTo("Juan Carlos");
        assertThat(result.getApellido()).isEqualTo("Pérez");
        assertThat(result.getDireccion()).isEqualTo("Dirección nueva");
        assertThat(result.getTelefono()).isEqualTo("3000000000");
        assertThat(result.getPassword()).isEqualTo("hashed-old");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void actualizarPerfilShouldHashPasswordWhenProvided() {
        User storedUser = User.builder().username("juan123").password("hashed-old").build();

        when(userRepositoryPort.findByUsername("juan123")).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.encode("nueva-clave")).thenReturn("hashed-new");
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.actualizarPerfil(
                "juan123", null, null, null, null, "nueva-clave");

        assertThat(result.getPassword()).isEqualTo("hashed-new");
    }

    @Test
    void actualizarPerfilShouldIgnoreBlankFields() {
        User storedUser = User.builder()
                .username("juan123")
                .nombre("Juan")
                .password("hashed-old")
                .build();

        when(userRepositoryPort.findByUsername("juan123")).thenReturn(Optional.of(storedUser));
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.actualizarPerfil(
                "juan123", "   ", "", null, "", "   ");

        assertThat(result.getNombre()).isEqualTo("Juan");
        assertThat(result.getPassword()).isEqualTo("hashed-old");
        verify(passwordEncoder, never()).encode(any());
    }
}

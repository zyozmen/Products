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
import com.zyozmen.products.domain.port.out.ReferralRepositoryPort;
import com.zyozmen.products.domain.model.ReferralPage;
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
import static org.mockito.ArgumentMatchers.anyString;
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

    @Mock
    private ReferralRepositoryPort referralRepositoryPort;

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
        when(userRepositoryPort.findByCodigoReferido("ABCD2345"))
                .thenReturn(Optional.of(User.builder().id("referrer").username("inviter").build()));

        User result = userService.registrar(request, "ABCD2345");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(captor.capture());

        assertThat(captor.getValue().getPassword()).isEqualTo("hashed-password");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.CLIENTE);
        assertThat(captor.getValue().getState()).isEqualTo(UserState.PENDIENTE);
        assertThat(captor.getValue().getCodigoReferido()).matches("[A-HJ-NP-Z2-9]{8}");
        assertThat(result.getRole()).isEqualTo(Role.CLIENTE);
        verify(referralRepositoryPort).insert(any());
        verify(userNotificationPort).sendWelcomeEmail(result);
    }

    @Test
    void registrarShouldRetryWhenGeneratedReferralCodeAlreadyExists() {
        User request = buildClienteRegistration();
        when(userRepositoryPort.existsByUsername("juan123")).thenReturn(false);
        when(userRepositoryPort.existsByTipoIdentificacionAndNumeroIdentificacion(
                IdentificationType.CC, "1018234567")).thenReturn(false);
        when(passwordEncoder.encode("raw-password")).thenReturn("hashed-password");
        when(userRepositoryPort.existsByCodigoReferido(anyString())).thenReturn(true, false);
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepositoryPort.findByCodigoReferido("ABCD2345"))
                .thenReturn(Optional.of(User.builder().id("referrer").username("inviter").build()));

        User result = userService.registrar(request, "ABCD2345");

        assertThat(result.getCodigoReferido()).matches("[A-HJ-NP-Z2-9]{8}");
        verify(userRepositoryPort, org.mockito.Mockito.times(2)).existsByCodigoReferido(anyString());
    }

    @Test
    void registrarShouldThrowWhenUsernameAlreadyExists() {
        User request = buildClienteRegistration();
        when(userRepositoryPort.existsByUsername("juan123")).thenReturn(true);

        assertThatThrownBy(() -> userService.registrar(request, "ABCD2345"))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("nombre de usuario ya está registrado");

        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    void registrarRequiresReferralCodeEvenWhenCalledOutsideHttp() {
        for (String code : new String[]{null, "", "   "}) {
            assertThatThrownBy(() -> userService.registrar(buildClienteRegistration(), code))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("obligatorio");
        }
        org.mockito.Mockito.verifyNoInteractions(userRepositoryPort, referralRepositoryPort);
    }

    @Test
    void registrarRejectsUnknownReferralCodeWithoutSaving() {
        assertThatThrownBy(() -> userService.registrar(buildClienteRegistration(), "UNKNOWN2"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no es valido");
        verify(userRepositoryPort, never()).save(any());
        org.mockito.Mockito.verifyNoInteractions(referralRepositoryPort, userNotificationPort);
    }

    @Test
    void registrarRejectsReferrerWithSameUsernameEmailOrIdentification() {
        var referrers = List.of(
                User.builder().id("r").username("juan123").build(),
                User.builder().id("r").email("JUAN@EXAMPLE.COM").build(),
                User.builder().id("r").tipoIdentificacion(IdentificationType.CC)
                        .numeroIdentificacion("1018234567").build());
        for (User referrer : referrers) {
            when(userRepositoryPort.findByCodigoReferido("ABCD2345")).thenReturn(Optional.of(referrer));
            assertThatThrownBy(() -> userService.registrar(buildClienteRegistration(), "ABCD2345"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("propio codigo");
        }
        verify(userRepositoryPort, never()).save(any());
        org.mockito.Mockito.verifyNoInteractions(referralRepositoryPort, userNotificationPort);
    }

    @Test
    void sameReferrerCodeCanBeUsedByMultipleNewUsers() {
        User referrer = User.builder().id("referrer").username("inviter").codigoReferido("ABCD2345").build();
        when(userRepositoryPort.findByCodigoReferido("ABCD2345")).thenReturn(Optional.of(referrer));
        when(userRepositoryPort.save(any())).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(saved.getUsername());
            return saved;
        });
        User first = buildClienteRegistration();
        User second = buildClienteRegistration();
        second.setUsername("another");
        second.setEmail("another@example.com");
        second.setNumeroIdentificacion("999");

        userService.registrar(first, "ABCD2345");
        userService.registrar(second, "ABCD2345");

        var links = ArgumentCaptor.forClass(com.zyozmen.products.domain.model.Referral.class);
        verify(referralRepositoryPort, org.mockito.Mockito.times(2)).insert(links.capture());
        assertThat(links.getAllValues()).extracting(com.zyozmen.products.domain.model.Referral::idReferente)
                .containsExactly("referrer", "referrer");
        assertThat(links.getAllValues()).extracting(com.zyozmen.products.domain.model.Referral::idReferido)
                .containsExactly("juan123", "another");
        assertThat(referrer.getCodigoReferido()).isEqualTo("ABCD2345");
    }

    @Test
    void listarReferidosScopesQueryToAuthenticatedUserId() {
        var expected = new ReferralPage(List.of(), 0, 20, 0);
        when(userRepositoryPort.findByUsername("juan123"))
                .thenReturn(Optional.of(User.builder().id("u1").username("juan123").build()));
        when(referralRepositoryPort.findByReferenteId("u1", 0, 20)).thenReturn(expected);

        assertThat(userService.listarReferidos("juan123", 0, 20)).isEqualTo(expected);
    }

    @Test
    void listarReferidosRejectsInvalidPaginationBeforeQuerying() {
        for (int[] pagination : new int[][]{{-1, 20}, {0, 0}, {0, 101}}) {
            assertThatThrownBy(() -> userService.listarReferidos("juan123", pagination[0], pagination[1]))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        org.mockito.Mockito.verifyNoInteractions(userRepositoryPort, referralRepositoryPort);
    }

    @Test
    void registrarShouldThrowWhenEmailAlreadyExists() {
        User request = buildClienteRegistration();
        when(userRepositoryPort.existsByUsername("juan123")).thenReturn(false);
        when(userRepositoryPort.existsByEmail("juan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registrar(request, "ABCD2345"))
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

        assertThatThrownBy(() -> userService.registrar(request, "ABCD2345"))
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

        assertThatThrownBy(() -> userService.registrar(request, "ABCD2345"))
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

        assertThatThrownBy(() -> userService.registrar(request, "ABCD2345"))
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
    void obtenerPorUsernameShouldAssignAndPersistReferralCodeForLegacyUser() {
        User storedUser = User.builder().username("juan123").build();
        when(userRepositoryPort.findByUsername("juan123")).thenReturn(Optional.of(storedUser));
        when(userRepositoryPort.existsByCodigoReferido(anyString())).thenReturn(false);
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.obtenerPorUsername("juan123");

        assertThat(result.getCodigoReferido()).matches("[A-HJ-NP-Z2-9]{8}");
        verify(userRepositoryPort).save(storedUser);
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
        User storedUser = User.builder()
                .username("juan123")
                .codigoReferido("ABCD2345")
                .build();
        when(userRepositoryPort.findByUsername("juan123")).thenReturn(Optional.of(storedUser));

        User result = userService.obtenerPorUsername("juan123");

        assertThat(result).isEqualTo(storedUser);
        assertThat(result.getCodigoReferido()).isEqualTo("ABCD2345");
        verify(userRepositoryPort, never()).existsByCodigoReferido(anyString());
        verify(userRepositoryPort, never()).save(any());
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

package com.zyozmen.products.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zyozmen.products.adapter.in.web.mapper.UserWebMapper;
import com.zyozmen.products.config.SecurityConfig;
import com.zyozmen.products.domain.model.ReferralPage;
import com.zyozmen.products.domain.model.User;
import com.zyozmen.products.domain.port.in.SessionUseCase;
import com.zyozmen.products.domain.port.in.UserUseCase;
import com.zyozmen.products.domain.port.out.RevokedTokenRepositoryPort;
import com.zyozmen.products.security.JwtAuthenticationFilter;
import com.zyozmen.products.security.JwtService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuthController.class, ReferralController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, UserWebMapper.class})
class ReferralRegistrationWebTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @MockBean private UserUseCase users;
    @MockBean private SessionUseCase sessions;
    @MockBean private JwtService jwt;
    @MockBean private RevokedTokenRepositoryPort revokedTokens;

    private static final String BODY = """
            {"username":"new-user","password":"secret","nombre":"Nuevo","apellido":"Usuario",
             "direccion":"Calle 1","telefono":"3001234567","email":"new@example.com",
             "tipoIdentificacion":"CC","numeroIdentificacion":"123","mayorDeEdad":true,
             "codigoReferido":"ABCD2345"}
            """;

    @Test
    void registrationRequiresCodeOnBothRoutes() throws Exception {
        for (String route : List.of("/api/auth/register", "/api/v1/auth/register")) {
            for (String code : new String[]{null, "", "   "}) {
                ObjectNode body = (ObjectNode) json.readTree(BODY);
                if (code == null) {
                    body.remove("codigoReferido");
                } else {
                    body.put("codigoReferido", code);
                }
                mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                        .andExpect(status().isBadRequest());
            }
        }
        verifyNoInteractions(users);
    }

    @Test
    void invalidCodeReturns400() throws Exception {
        when(users.registrar(any(User.class), eq("ABCD2345")))
                .thenThrow(new IllegalArgumentException("El codigo de referido no es valido"));
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El codigo de referido no es valido"));
    }

    @Test
    void versionedRegistrationPassesInviterCodeSeparatelyAndReturnsOwnCode() throws Exception {
        when(users.registrar(any(User.class), eq("ABCD2345")))
                .thenAnswer(invocation -> {
                    User newUser = invocation.getArgument(0);
                    org.assertj.core.api.Assertions.assertThat(newUser.getCodigoReferido()).isNull();
                    return User.builder().username("new-user").codigoReferido("WXYZ6789").build();
                });
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoReferido").value("WXYZ6789"));
    }

    @Test
    void bothReferralRoutesRequireAuthentication() throws Exception {
        for (String route : List.of("/api/v1/users/me/referrals", "/api/v1/users/profile/referrals")) {
            mvc.perform(get(route)).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(users);
    }

    @Test
    void invalidTokenCannotAccessReferrals() throws Exception {
        mvc.perform(get("/api/v1/users/me/referrals").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(users);
    }

    @Test
    void invalidPaginationReturns400() throws Exception {
        when(users.listarReferidos("inviter", -1, 20))
                .thenThrow(new IllegalArgumentException("page debe ser >= 0 y size debe estar entre 1 y 100"));
        mvc.perform(get("/api/v1/users/me/referrals").with(user("inviter")).param("page", "-1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/users/me/referrals").with(user("inviter")).param("page", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bothRoutesReturnOnlyOwnReferralsAndSafeProjection() throws Exception {
        var item = new ReferralPage.Item("new-user", "Nuevo", "Usuario", Instant.parse("2026-11-09T15:00:00Z"));
        when(users.listarReferidos("inviter", 1, 2))
                .thenReturn(new ReferralPage(List.of(item), 1, 2, 3));
        for (String route : List.of("/api/v1/users/me/referrals", "/api/v1/users/profile/referrals")) {
            mvc.perform(get(route).with(user("inviter").roles("CLIENTE"))
                            .param("page", "1").param("size", "2").param("username", "other"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].username").value("new-user"))
                    .andExpect(jsonPath("$.content[0].createdAt").value("2026-11-09T15:00:00Z"))
                    .andExpect(jsonPath("$.content[0].email").doesNotExist())
                    .andExpect(jsonPath("$.content[0].password").doesNotExist())
                    .andExpect(jsonPath("$.content[0].codigoReferido").doesNotExist())
                    .andExpect(jsonPath("$.content[0].numeroIdentificacion").doesNotExist())
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.page").value(1));
        }
        verify(users, times(2)).listarReferidos("inviter", 1, 2);
    }

    @Test
    void emptyPageHasZeroTotalsAndDefaultPagination() throws Exception {
        when(users.listarReferidos("inviter", 0, 20)).thenReturn(new ReferralPage(List.of(), 0, 20, 0));
        mvc.perform(get("/api/v1/users/me/referrals").with(user("inviter")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalPages").value(0));
    }
}

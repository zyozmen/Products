package com.zyozmen.products.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private com.zyozmen.products.domain.port.out.RevokedTokenRepositoryPort revokedTokenPort;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSetAuthenticationWhenTokenIsValid() throws Exception {
        JwtAuthenticationFilter filterUnderTest = new JwtAuthenticationFilter(jwtService, revokedTokenPort);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractUsername("valid-token")).thenReturn("juan123");
        when(jwtService.extractRole("valid-token")).thenReturn("CLIENTE");

        filterUnderTest.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(authentication.getName()).isEqualTo("juan123");
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_CLIENTE");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotSetAuthenticationWhenNoAuthorizationHeader() throws Exception {
        JwtAuthenticationFilter filterUnderTest = new JwtAuthenticationFilter(jwtService, revokedTokenPort);
        when(request.getHeader("Authorization")).thenReturn(null);

        filterUnderTest.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotSetAuthenticationWhenTokenIsInvalid() throws Exception {
        JwtAuthenticationFilter filterUnderTest = new JwtAuthenticationFilter(jwtService, revokedTokenPort);
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtService.isTokenValid("invalid-token")).thenReturn(false);

        filterUnderTest.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(request).setAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE, "SESSION_EXPIRED");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotSetAuthenticationWhenTokenIsRevoked() throws Exception {
        JwtAuthenticationFilter filterUnderTest = new JwtAuthenticationFilter(jwtService, revokedTokenPort);
        when(request.getHeader("Authorization")).thenReturn("Bearer revoked-token");
        when(jwtService.isTokenValid("revoked-token")).thenReturn(true);
        when(jwtService.extractJti("revoked-token")).thenReturn("jti-1");
        when(revokedTokenPort.isRevoked("jti-1")).thenReturn(true);

        filterUnderTest.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(request).setAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE, "SESSION_EXPIRED");
        verify(filterChain).doFilter(request, response);
    }
}

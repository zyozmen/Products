package com.zyozmen.products.security;

import com.zyozmen.products.domain.port.out.RevokedTokenRepositoryPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro que intercepta cada petición HTTP, extrae el token Bearer del
 * encabezado Authorization, lo valida y -si es válido- establece la
 * autenticación en el SecurityContext con el rol del usuario como
 * autoridad (prefijo ROLE_), para que @PreAuthorize / hasRole funcionen.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTH_ERROR_ATTRIBUTE = "auth.error";
    public static final String SESSION_EXPIRED = "SESSION_EXPIRED";

    private final JwtService jwtService;
    private final RevokedTokenRepositoryPort revokedTokenPort;

    public JwtAuthenticationFilter(JwtService jwtService, RevokedTokenRepositoryPort revokedTokenPort) {
        this.jwtService = jwtService;
        this.revokedTokenPort = revokedTokenPort;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());

            if (!jwtService.isTokenValid(token) || revokedTokenPort.isRevoked(jwtService.extractJti(token))) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, SESSION_EXPIRED);
            } else if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String username = jwtService.extractUsername(token);
                String role = jwtService.extractRole(token);

                if (username != null && role != null) {
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    ((UsernamePasswordAuthenticationToken) authentication)
                            .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}

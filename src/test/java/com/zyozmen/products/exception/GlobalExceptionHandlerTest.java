package com.zyozmen.products.exception;

import com.zyozmen.products.domain.exception.InvalidCredentialsException;
import com.zyozmen.products.domain.exception.MainAdminProtectedException;
import com.zyozmen.products.domain.exception.ResourceNotFoundException;
import com.zyozmen.products.domain.exception.ServiceUnavailableException;
import com.zyozmen.products.domain.exception.UnderageRegistrationException;
import com.zyozmen.products.domain.exception.UserAlreadyExistsException;
import com.zyozmen.products.domain.exception.UserInactiveException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleServiceUnavailableExceptionShouldReturn503Response() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/productos");

        ResponseEntity<ErrorResponse> response = handler.handleServiceUnavailableException(
                new ServiceUnavailableException("Service unavailable"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(503);
        assertThat(response.getBody().getError()).isEqualTo("Service Unavailable");
        assertThat(response.getBody().getPath()).isEqualTo("/api/productos");
    }

    @Test
    void handleResourceNotFoundExceptionShouldReturn404Response() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/productos/99");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFoundException(
                new ResourceNotFoundException("Producto no encontrado"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("Producto no encontrado");
        assertThat(response.getBody().getPath()).isEqualTo("/api/productos/99");
    }

    @Test
    void handleValidationExceptionShouldReturn400ResponseWithValidationErrors() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/productos");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("producto", "name", "El nombre es obligatorio")
        ));

        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getValidationErrors()).containsExactly("El nombre es obligatorio");
    }

    @Test
    void handleGenericExceptionShouldReturn500Response() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/productos");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(
                new RuntimeException("Unexpected error"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("Unexpected error");
        assertThat(response.getBody().getPath()).isEqualTo("/api/productos");
    }

    @Test
    void handleBadRequestDomainExceptionShouldReturn400ForUserAlreadyExists() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/auth/register");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequestDomainException(
                new UserAlreadyExistsException("El nombre de usuario ya está registrado"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("El nombre de usuario ya está registrado");
    }

    @Test
    void handleBadRequestDomainExceptionShouldReturn400ForUnderageRegistration() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/auth/register");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequestDomainException(
                new UnderageRegistrationException("Debe ser mayor de edad"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Debe ser mayor de edad");
    }

    @Test
    void handleBadRequestDomainExceptionShouldReturn400ForMainAdminProtected() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/admin/users/admin/toggle-status");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequestDomainException(
                new MainAdminProtectedException("No se puede inactivar al administrador principal"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("No se puede inactivar al administrador principal");
    }

    @Test
    void handleInvalidCredentialsExceptionShouldReturn401Response() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidCredentialsException(
                new InvalidCredentialsException("Credenciales inválidas"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Credenciales inválidas");
    }

    @Test
    void handleUserInactiveExceptionShouldReturn403ResponseWithUserInactiveShape() {
        ResponseEntity<UserInactiveErrorResponse> response = handler.handleUserInactiveException(
                new UserInactiveException("El usuario está inactivo. Por favor contacte al administrador"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("user_inactive");
        assertThat(response.getBody().message())
                .isEqualTo("El usuario está inactivo. Por favor contacte al administrador");
    }
}

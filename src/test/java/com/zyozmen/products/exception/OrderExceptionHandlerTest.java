package com.zyozmen.products.exception;

import com.zyozmen.products.domain.exception.OrderConflictException;
import com.zyozmen.products.domain.exception.OrderProductException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderExceptionHandlerTest {

    private final OrderExceptionHandler handler = new OrderExceptionHandler();

    @Test
    void mapsValidationErrorsAndUsesFallbackWhenNoFieldErrorsExist() {
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException validation = mock(MethodArgumentNotValidException.class);
        when(validation.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("order", "items", "Debe incluir productos")));

        assertThat(handler.handleValidation(validation).getBody())
                .containsEntry("message", "Debe incluir productos");

        when(bindingResult.getFieldErrors()).thenReturn(List.of());
        assertThat(handler.handleValidation(validation).getBody())
                .containsEntry("message", "Error de validación en los datos de entrada");
    }

    @Test
    void mapsOrderErrorsToTheirHttpStatuses() {
        assertThat(handler.handleInvalidProduct(new OrderProductException("producto inválido")).getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(handler.handleConflict(new OrderConflictException("conflicto")).getBody())
                .containsEntry("message", "conflicto");
        assertThat(handler.handleConflict(new org.springframework.dao.OptimisticLockingFailureException("stale"))
                .getBody()).containsEntry("message",
                "La orden fue modificada por otra solicitud; vuelve a intentarlo");
        assertThat(handler.handleInvalidState(new IllegalStateException("estado inválido")).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}

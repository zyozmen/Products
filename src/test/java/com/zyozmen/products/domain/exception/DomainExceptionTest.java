package com.zyozmen.products.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionTest {

    @Test
    void preservesMessagesAcrossDomainExceptionTypes() {
        String message = "error de dominio";

        assertThat(new BusinessRuleException(message)).hasMessage(message);
        assertThat(new CapacidadExcedidaException(message)).hasMessage(message);
        assertThat(new InvalidCredentialsException(message)).hasMessage(message);
        assertThat(new MainAdminProtectedException(message)).hasMessage(message);
        assertThat(new MargenNegativoException(message)).hasMessage(message);
        assertThat(new OrderConflictException(message)).hasMessage(message);
        assertThat(new OrderProductException(message)).hasMessage(message);
        assertThat(new OrderValidationException(message)).hasMessage(message);
        assertThat(new ResourceNotFoundException(message)).hasMessage(message);
        assertThat(new SessionExpiredException(message)).hasMessage(message);
        assertThat(new UnderageRegistrationException(message)).hasMessage(message);
        assertThat(new UserAlreadyExistsException(message)).hasMessage(message);
        assertThat(new UserInactiveException(message)).hasMessage(message);
        assertThat(new UserPendingApprovalException(message)).hasMessage(message);
    }

    @Test
    void serviceUnavailablePreservesMessageAndCause() {
        String message = "servicio no disponible";
        RuntimeException cause = new RuntimeException("causa");

        assertThat(new ServiceUnavailableException(message)).hasMessage(message);
        assertThat(new ServiceUnavailableException(message, cause))
                .hasMessage(message)
                .hasCause(cause);
    }
}

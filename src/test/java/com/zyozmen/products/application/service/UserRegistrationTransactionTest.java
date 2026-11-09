package com.zyozmen.products.application.service;

import com.zyozmen.products.domain.model.User;
import com.zyozmen.products.domain.port.in.UserUseCase;
import com.zyozmen.products.domain.port.out.ReferralRepositoryPort;
import com.zyozmen.products.domain.port.out.UserNotificationPort;
import com.zyozmen.products.domain.port.out.UserRepositoryPort;
import com.zyozmen.products.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserRegistrationTransactionTest {
    @Test
    void welcomeNotificationWaitsForTransactionCommit() {
        var users = mock(UserRepositoryPort.class);
        var referrals = mock(ReferralRepositoryPort.class);
        var notifications = mock(UserNotificationPort.class);
        when(users.findByCodigoReferido("ABCD2345"))
                .thenReturn(Optional.of(User.builder().id("referrer").username("inviter").build()));
        User saved = User.builder().id("new-user").build();
        when(users.save(any())).thenReturn(saved);
        var service = new UserService(users, mock(PasswordEncoder.class), mock(JwtService.class),
                notifications, referrals);
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.registrar(User.builder().username("new").password("raw").mayorDeEdad(true).build(),
                    "ABCD2345");
            verifyNoInteractions(notifications);
            verify(referrals).insert(any());
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            verify(notifications).sendWelcomeEmail(saved);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void associationFailureRollsBackUserCreationAndDoesNotNotify() {
        var users = mock(UserRepositoryPort.class);
        var referrals = mock(ReferralRepositoryPort.class);
        var notifications = mock(UserNotificationPort.class);
        var encoder = mock(PasswordEncoder.class);
        when(users.findByCodigoReferido("ABCD2345"))
                .thenReturn(Optional.of(User.builder().id("referrer").username("inviter").build()));
        when(users.save(any())).thenReturn(User.builder().id("new-user").build());
        doThrow(new DuplicateKeyException("idReferido duplicado")).when(referrals).insert(any());
        var manager = mock(PlatformTransactionManager.class);
        var status = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(status);
        var interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(manager);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var factory = new ProxyFactory(new UserService(users, encoder, mock(JwtService.class),
                notifications, referrals));
        factory.addAdvice(interceptor);
        var service = (UserUseCase) factory.getProxy();
        User request = User.builder().username("new").password("raw").mayorDeEdad(true).build();

        assertThatThrownBy(() -> service.registrar(request, "ABCD2345"))
                .isInstanceOf(DuplicateKeyException.class);

        verify(users).save(any());
        verify(manager).rollback(status);
        verify(manager, never()).commit(any());
        verifyNoInteractions(notifications);
    }
}

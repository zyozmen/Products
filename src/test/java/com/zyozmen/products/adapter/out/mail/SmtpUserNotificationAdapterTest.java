package com.zyozmen.products.adapter.out.mail;

import com.zyozmen.products.domain.model.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SmtpUserNotificationAdapterTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    private User user(String email) {
        return User.builder().username("juan123").nombre("Juan").email(email).build();
    }

    private SmtpUserNotificationAdapter adapter(boolean enabled) {
        return new SmtpUserNotificationAdapter(mailSender, enabled, "zonagreen9@gmail.com", "Bienvenido");
    }

    @Test
    void sendsWelcomeEmailFromConfiguredSender() {
        adapter(true).sendWelcomeEmail(user("juan@example.com"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("zonagreen9@gmail.com");
        assertThat(message.getTo()).containsExactly("juan@example.com");
        assertThat(message.getSubject()).isEqualTo("Bienvenido");
        assertThat(message.getText()).contains("Juan").contains("juan123");
    }

    @Test
    void doesNotSendWhenDisabledOrEmailMissing() {
        adapter(false).sendWelcomeEmail(user("juan@example.com"));
        adapter(true).sendWelcomeEmail(user(" "));
        adapter(true).sendWelcomeEmail(user(null));
        adapter(true).sendWelcomeEmail(null);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void swallowsMailFailures() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> adapter(true).sendWelcomeEmail(user("juan@example.com")))
                .doesNotThrowAnyException();
    }
}

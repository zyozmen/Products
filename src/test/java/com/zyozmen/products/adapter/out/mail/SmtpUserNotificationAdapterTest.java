package com.zyozmen.products.adapter.out.mail;

import com.zyozmen.products.domain.model.User;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpUserNotificationAdapterTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenAnswer(inv -> new MimeMessage((Session) null));
    }

    private User user(String email) {
        return User.builder().username("juan123").nombre("Juan").email(email).build();
    }

    private SmtpUserNotificationAdapter adapter(boolean enabled) {
        return new SmtpUserNotificationAdapter(mailSender, enabled, "zonagreen9@gmail.com", "Bienvenido");
    }

    @Test
    void sendsWelcomeEmailFromConfiguredSender() throws Exception {
        adapter(true).sendWelcomeEmail(user("juan@example.com"));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage message = captor.getValue();
        assertThat(message.getFrom()[0].toString()).isEqualTo("zonagreen9@gmail.com");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("juan@example.com");
        assertThat(message.getSubject()).isEqualTo("Bienvenido");
        message.saveChanges();
        assertThat(message.getContentType()).contains("text/html");
    }

    @Test
    void doesNotSendWhenDisabledOrEmailMissing() {
        adapter(false).sendWelcomeEmail(user("juan@example.com"));
        adapter(true).sendWelcomeEmail(user(" "));
        adapter(true).sendWelcomeEmail(user(null));
        adapter(true).sendWelcomeEmail(null);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void swallowsMailFailures() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> adapter(true).sendWelcomeEmail(user("juan@example.com")))
                .doesNotThrowAnyException();
    }
}

package com.zyozmen.products.adapter.out.mail;

import com.zyozmen.products.domain.model.User;
import com.zyozmen.products.domain.port.out.UserNotificationPort;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Adaptador de salida SMTP para notificaciones a usuarios.
 * El envío es asíncrono y tolerante a fallos: un error de SMTP se registra
 * pero no afecta al registro del usuario.
 */
@Component
public class SmtpUserNotificationAdapter implements UserNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(SmtpUserNotificationAdapter.class);

    private static final Resource TEMPLATE = new ClassPathResource("email/email_template.html");

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;
    private final String subject;

    public SmtpUserNotificationAdapter(
            JavaMailSender mailSender,
            @Value("${app.mail.enabled:true}") boolean enabled,
            @Value("${app.mail.from:zonagreen9@gmail.com}") String from,
            @Value("${app.mail.welcome-subject:Bienvenido a GrowShop}") String subject) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
        this.subject = subject;
    }

    @Override
    @Async
    public void sendWelcomeEmail(User user) {
        if (!enabled || user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(buildBody(), true);
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("No se pudo enviar el correo de bienvenida al usuario {}: {}",
                    user.getUsername(), ex.getMessage());
        }
    }

    private String buildBody() throws IOException {
        return TEMPLATE.getContentAsString(StandardCharsets.UTF_8);
    }
}

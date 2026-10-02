package com.zyozmen.products.config;

import com.zyozmen.products.domain.model.Role;
import com.zyozmen.products.domain.model.TipoIdentificacion;
import com.zyozmen.products.domain.model.User;
import com.zyozmen.products.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Siembra (seeding) del usuario administrador principal del sistema al
 * arrancar la aplicación, si todavía no existe. Las credenciales pueden
 * configurarse con las variables de entorno ADMIN_USERNAME / ADMIN_PASSWORD;
 * por defecto usa admin/admin (solo recomendado para desarrollo).
 */
@Component
@RequiredArgsConstructor
public class AdminUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepositoryPort.existsByUsername(adminUsername)) {
            return;
        }

        User admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .nombre("Administrador")
                .apellido("Sistema")
                .direccion("Calle Principal 123")
                .telefono("3124058166")
                .tipoIdentificacion(TipoIdentificacion.CC)
                .numeroIdentificacion("11111111")
                .mayorDeEdad(true)
                .active(true)
                .role(Role.ADMIN)
                .build();

        userRepositoryPort.save(admin);
        log.info("Usuario administrador por defecto creado: {}", adminUsername);
    }
}

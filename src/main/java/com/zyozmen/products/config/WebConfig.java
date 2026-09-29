package com.zyozmen.products.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:4200,http://localhost:8080,http://127.0.0.1:3000,http://127.0.0.1:4200,http://127.0.0.1:8080}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        CorsRegistration registration = registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .maxAge(3600);

        if (origins.stream().anyMatch("*"::equals)) {
            registration.allowedOriginPatterns("*");
            return;
        }

        if (origins.isEmpty()) {
            registration.allowedOriginPatterns("http://localhost:[*]", "http://127.0.0.1:[*]", "https://localhost:[*]", "https://127.0.0.1:[*]");
            return;
        }

        registration.allowedOriginPatterns(origins.toArray(String[]::new));
    }
}

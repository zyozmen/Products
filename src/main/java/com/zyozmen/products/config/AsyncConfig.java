package com.zyozmen.products.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Habilita la ejecución asíncrona (@Async), usada para el envío de correos.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}

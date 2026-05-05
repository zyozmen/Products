package com.zyozmen.products;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de la aplicación.
 *
 * @SpringBootApplication combina tres anotaciones:
 *   - @Configuration: marca esta clase como fuente de beans de Spring.
 *   - @EnableAutoConfiguration: activa la autoconfiguración de Spring Boot.
 *   - @ComponentScan: habilita el escaneo de componentes en este paquete y subpaquetes.
 */
@SpringBootApplication
public class ProductsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductsApplication.class, args);
    }
}

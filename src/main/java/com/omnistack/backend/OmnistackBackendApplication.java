package com.omnistack.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada principal del backend OMNISTACK.
 */
@EnableScheduling
@SpringBootApplication
public class OmnistackBackendApplication {

    /**
     * Inicializa la aplicacion Spring Boot.
     *
     * @param args argumentos de linea de comandos
     */
    public static void main(String[] args) {
        SpringApplication.run(OmnistackBackendApplication.class, args);
    }
}

package com.example.backend_sirhgp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée principal de l'application SIRHGP.
 *
 * @EnableScheduling est requis pour le nettoyage automatique
 * de la blacklist des tokens JWT dans TokenBlacklistService.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class BackendSirhgpApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendSirhgpApplication.class, args);
    }
}
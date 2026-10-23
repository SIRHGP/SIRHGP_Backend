// ─── DatabaseConfig.java ──────────────────────────────────────────────────────
package com.example.backend_sirhgp.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Valide au démarrage que les identifiants MySQL sont réellement fournis.
 * Sans cela, Spring passerait le littéral "${DB_USERNAME}" au pilote JDBC et
 * MySQL renverrait une erreur d'authentification cryptique.
 *
 * <p>Convention (voir doc/REFERENCE-SIRHGP.md §1.7) : {@code DB_USERNAME} et
 * {@code DB_PASSWORD} sont requises dans tous les profils. Aucune valeur par
 * d�faut n'est cod�e en dur dans le d�p�t.
 */
@Configuration
public class DatabaseConfig {

    @Value("${DB_USERNAME:}")
    private String dbUsername;

    @Value("${DB_PASSWORD:}")
    private String dbPassword;

    private final Environment environment;

    public DatabaseConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateDatabaseCredentials() {
        String activeProfiles = String.join(",", environment.getActiveProfiles());

        if (dbUsername == null || dbUsername.isBlank() || dbUsername.contains("${")) {
            throw new IllegalStateException(
                "DB_USERNAME n'est pas d�fini. D�finissez la variable d'environnement DB_USERNAME "
                + "(exemple : DB_USERNAME=root) avant de d�marrer le backend (profil(s) actif(s) : "
                + activeProfiles + ")."
            );
        }

        if (dbPassword == null || dbPassword.isBlank() || dbPassword.contains("${")) {
            throw new IllegalStateException(
                "DB_PASSWORD n'est pas d�fini. D�finissez la variable d'environnement DB_PASSWORD "
                + "avant de d�marrer le backend (profil(s) actif(s) : " + activeProfiles + ")."
            );
        }
    }
}
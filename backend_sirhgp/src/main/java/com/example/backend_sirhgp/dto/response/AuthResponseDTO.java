package com.example.backend_sirhgp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Réponse au login : token JWT + infos essentielles de l'utilisateur.
 * CORRECTION : on n'inclut pas EmployerResponseDTO complet ici pour éviter
 * la sur-sérialisation. Juste les infos nécessaires au frontend pour démarrer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    private String token;
    private String tokenType = "Bearer";
    private Long expiresIn;       // durée en ms
    private UserSummaryDTO user;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummaryDTO {
        private Long id;
        private String name;
        private String email;
        private String roleName;
        private String posteName;
        private String departementName;
        private boolean actif;
    }
}
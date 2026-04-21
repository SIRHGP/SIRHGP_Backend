package com.example.backend_sirhgp.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentaireResponseDTO {

    private Long id;
    private String contenu;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private Boolean estModifie;

    private Long tacheId;

    // Auteur : résumé
    private AuteurSummaryDTO auteur;

    @Data
    public static class AuteurSummaryDTO {
        private Long id;
        private String nom;
        private String email;
        private String photo;
    }
}
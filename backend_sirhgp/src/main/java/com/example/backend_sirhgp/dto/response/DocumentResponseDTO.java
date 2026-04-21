package com.example.backend_sirhgp.dto.response;

import com.example.backend_sirhgp.model.Document;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * CORRECTION : MultipartFile retiré — ce DTO est une réponse pure,
 * pas un DTO d'upload. Le téléchargement se fait via un endpoint
 * GET /documents/{id}/download qui retourne un Resource.
 */
@Data
public class DocumentResponseDTO {

    private Long id;
    private String nom;
    private String description;
    private Document.TypeDocument type;
    private boolean publique;

    private String nomFichier;
    private String typeMime;
    private Long taille;
    private String tailleFormatee;
    private String extension;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private Long nombreTelechargements;

    // Contexte — résumés seulement
    private Long employerId;
    private String employerNom;

    private Long uploaderId;
    private String uploaderNom;
    private String uploaderEmail;

    private Long projetId;
    private String projetNom;
}
package com.example.backend_sirhgp.dto.response;

import com.example.backend_sirhgp.model.Document.DocumentStatus;
import com.example.backend_sirhgp.model.Document.TypeDocument;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentResponseDTO {
    private Long id;
    private String nom;
    private String description;
    private String nomFichier;
    private String typeMime;
    private Long taille;
    private String tailleFormatee;
    private String extension;
    private TypeDocument type;
    private DocumentStatus status;
    private boolean publique;
    private Long nombreTelechargements;

    // Infos expéditeur
    private Long expediteurId;
    private String expediteurNom;
    private String expediteurEmail;

    // Infos destinataire
    private Long destinataireId;
    private String destinataireNom;
    private String destinataireEmail;

    // Métadonnées de partage
    private String message;
    private boolean estLu;
    private boolean estArchive;
    private LocalDateTime dateCreation;
    private LocalDateTime dateEnvoi;
    private LocalDateTime dateLecture;

    // Relations
    private Long employerId;
    private String employerNom;
    private Long projetId;
    private String projetNom;
}
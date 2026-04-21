package com.example.backend_sirhgp.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DocumentPartageResponseDTO {

    private Long id;

    // Résumé du document partagé
    private Long documentId;
    private String documentNom;
    private String documentType;
    private Long documentTaille;
    private String documentTypeMime;

    // Expéditeur
    private Long expediteurId;
    private String expediteurNom;
    private String expediteurEmail;

    // Destinataire
    private Long destinataireId;
    private String destinataireNom;
    private String destinataireEmail;

    private String message;
    private boolean lu;
    private LocalDateTime dateEnvoi;
}
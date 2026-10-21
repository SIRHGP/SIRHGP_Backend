// ─── EntrepriseResponseDTO.java ──────────────────────────────────────────────
package com.example.backend_sirhgp.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EntrepriseResponseDTO {

    private Long id;
    private String name;
    private String description;
    private String secteurActivite;
    private String type; // TypeEntreprise
    private java.time.LocalTime heureArriveeStandard;
    private java.time.LocalTime heureDepartStandard;
    private java.time.LocalTime heureArriveeMinimale;
    private String adresse;
    private String ville;
    private String telephone;
    private String email;
    private String logoUrl;
    private String rccm;
    private String numeroCnpsEmployeur;
    private String statut;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    // Compteurs enrichis (facultatifs)
    private Long nombreEmployes;
}
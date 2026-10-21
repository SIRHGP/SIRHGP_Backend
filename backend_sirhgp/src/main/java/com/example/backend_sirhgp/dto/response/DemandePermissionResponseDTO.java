package com.example.backend_sirhgp.dto.response;

import com.example.backend_sirhgp.model.DemandePermission.StatutPermission;
import com.example.backend_sirhgp.model.DemandePermission.TypePermission;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Refonte 5.6 — Vue d'une demande de permission.
 * Mappée manuellement dans DemandePermissionService (entité simple → pas de MapStruct).
 */
@Data
public class DemandePermissionResponseDTO {

    private Long id;
    private TypePermission type;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private String motif;
    private StatutPermission statut;

    // Demandeur (résumé — fetch-join, jamais lazy ici)
    private Long demandeurId;
    private String demandeurNom;

    // Valideur snapshoté
    private Long valideurId;
    private String valideurNom;
    private String commentaireValideur;

    // Justificatif (lien de téléchargement prêt à l'emploi)
    private Long justificatifId;
    private String justificatifNom;
    private String justificatifUrl;

    // Entreprise du demandeur (traçabilité multi-tenant)
    private Long entrepriseId;

    private LocalDateTime dateCreation;
    private LocalDateTime dateDecision;

    /** Durée calculée (jours calendaires, 1 si SORTIE sur un seul jour). */
    private Long dureeJours;
}

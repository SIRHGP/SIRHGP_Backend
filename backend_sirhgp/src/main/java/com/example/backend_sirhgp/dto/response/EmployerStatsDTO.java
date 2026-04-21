package com.example.backend_sirhgp.dto.response;

import lombok.Data;
import java.util.Map;

/**
 * DTO de statistiques d'un employé — chargé séparément via GET /employers/{id}/stats
 * CORRECTION : séparé de EmployerResponseDTO pour ne pas surcharger chaque réponse
 * avec des données calculées coûteuses.
 */
@Data
public class EmployerStatsDTO {

    private Long employerId;
    private String employerNom;

    // Tâches
    private Long nombreTaches;
    private Long nombreTachesTerminees;
    private Long nombreTachesEnCours;
    private Long nombreTachesEnRetard;
    private Map<String, Long> tachesParMois;

    // Événements
    private Long nombreEvenements;
    private Long nombreEvenementsMois;
    private Long nombreEvenementsSemaine;
    private Map<String, Long> evenementsParMois;

    // Documents & Projets
    private Long nombreDocuments;
    private Long nombreProjets;

    // Factures
    private Long nombreFacturesEmises;
    private Long nombreFacturesRecues;
    private java.math.BigDecimal montantTotalFacturesEmises;
    private java.math.BigDecimal montantTotalFacturesRecues;
}
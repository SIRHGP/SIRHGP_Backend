package com.example.backend_sirhgp.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * [3.5] Rapport "prévu vs réalisé" d'un projet.
 */
@Data
public class ProjetReportingDTO {

    private Long projetId;
    private String nom;
    private String codeProjet;
    private String statut;

    // ─── Budget ──────────────────────────────────────────────
    private BigDecimal budgetPrevu;
    private BigDecimal coutReel;
    private BigDecimal ecartBudget;
    private Double tauxConsommation;

    // ─── Délais ──────────────────────────────────────────────
    private LocalDate dateDebut;
    private LocalDate dateFinPrevue;
    private LocalDate dateFinReelle;
    private Long retardJours;       // > 0 si terminé après la date prévue, ou non terminé et échéance dépassée
    private Boolean estEnRetard;

    // ─── Progression ─────────────────────────────────────────
    private Integer progression;
    private Long tachesTotal;
    private Long tachesTerminees;
    private Long tachesEnRetard;
    private Long tachesBloquees;
    /** Répartition par statut (A_FAIRE → nb, EN_COURS → nb, ...) */
    private Map<String, Long> tachesParStatut;
}

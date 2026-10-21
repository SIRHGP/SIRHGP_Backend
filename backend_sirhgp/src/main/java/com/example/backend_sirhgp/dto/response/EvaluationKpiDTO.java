package com.example.backend_sirhgp.dto.response;

import lombok.Data;

/**
 * KPI d'un évalué (refonte 6.1) — alimente le formulaire d'évaluation et le
 * rapport figé. Chargé via GET /api/evaluations/kpi/{employeId}.
 */
@Data
public class EvaluationKpiDTO {

    private Long employeId;
    private String employeNom;

    // Tâches (global)
    private Long tachesTotal;
    private Long tachesTerminees;
    private Long tachesEnCours;
    private Long tachesEnRetard;
    /** Taux de complétion en % (terminées / total). */
    private Long tauxCompletion;

    // Assiduité (30 derniers jours ouvrés)
    private Long joursOuvres;
    private Long joursPointes;
    private Long retards;
    private Long absences;
}
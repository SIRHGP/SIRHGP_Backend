// ─── EvaluationResponseDTO.java ────────────────────────────────────────────────
package com.example.backend_sirhgp.dto.response;

import com.example.backend_sirhgp.dto.request.CritereEvaluationDTO;
import com.example.backend_sirhgp.model.Evaluation;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Évaluation renvoyée à l'UI (2.4, refonte 6.1). */
@Data
public class EvaluationResponseDTO {

    private Long id;
    private Evaluation.TypeEvaluation type;
    /** Catégorie / décision : PROMOTION / RENVOI / RECADREMENT (6.1). */
    private Evaluation.CategorieEvaluation categorie;
    /** Objet libre de l'évaluation (6.1). */
    private String objet;
    private Integer annee;
    private String periode;
    private Evaluation.StatutEvaluation statut;
    private LocalDate dateEvaluation;
    private String commentaire;
    private BigDecimal noteGlobale;

    /** Rapport figé à la validation (récap auto) (6.1). */
    private String rapport;
    private LocalDate dateRapport;

    // Les 2 interpellés (6.1) : évaluateur + évalué
    private Long evalueId;
    private String evalueName;
    private String evalueRole;

    private Long evaluateurId;
    private String evaluateurName;
    private String evaluateurRole;

    /**
     * MANAGER destinataire de la validation (résolu à la soumission, 6.1) —
     * sert de cible de notification LEAD → MANAGER.
     */
    private Long valideurId;
    private String valideurNom;

    private List<CritereEvaluationDTO> criteres;
}
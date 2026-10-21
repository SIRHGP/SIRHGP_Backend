// ─── EvaluationRequestDTO.java ─────────────────────────────────────────────────
package com.example.backend_sirhgp.dto.request;

import com.example.backend_sirhgp.dto.request.CritereEvaluationDTO;
import com.example.backend_sirhgp.model.Evaluation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Requête de création d'une évaluation (2.4, refonte 6.1).
 *
 * <p>Deux champs obligatoires distincts : {@code type} (fréquence) et
 * {@code categorie} (PROMOTION / RENVOI / RECADREMENT) + un objet libre.
 * L'évaluateur n'est plus choisi : il est <b>forcément l'utilisateur courant</b>
 * (cf. EvaluationService.create) — le select disparaît de l'écran.
 */
@Data
public class EvaluationRequestDTO {

    @NotNull(message = "Le type d'évaluation est obligatoire")
    private Evaluation.TypeEvaluation type;

    @NotNull(message = "La catégorie d'évaluation est obligatoire")
    private Evaluation.CategorieEvaluation categorie;

    @NotBlank(message = "L'objet de l'évaluation est obligatoire")
    private String objet;

    private Integer annee;
    private String periode;

    private String commentaire;
    private LocalDate dateEvaluation;

    @NotNull(message = "L'employé évalué est obligatoire")
    private Long evalueId;

    /**
     * Ignoré : conservé pour compatibilité avec les anciens clients, la valeur
     * réelle est toujours l'utilisateur connecté (anti usurpation d'évaluateur).
     */
    private Long evaluateurId;

    @Valid
    private List<CritereEvaluationDTO> criteres;
}
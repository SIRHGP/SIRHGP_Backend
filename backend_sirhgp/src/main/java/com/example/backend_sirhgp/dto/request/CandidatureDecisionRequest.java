package com.example.backend_sirhgp.dto.request;

import com.example.backend_sirhgp.model.Candidature;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Pré-évaluation du dossier candidat (4.3) : verdict + note + avis + CV lié.
 */
@Data
public class CandidatureDecisionRequest {

    private Candidature.DecisionRecrutement decision; // PENDING / A_EVALUER / EN_STAGE / ENGAGE / REFUSE_AVANT

    @DecimalMin(value = "0.0", message = "La note doit être ≥ 0")
    @DecimalMax(value = "20.0", message = "La note doit être ≤ 20")
    private BigDecimal note;

    private String avis;

    private Long documentId; // lien vers le CV téléversé (optionnel)
}
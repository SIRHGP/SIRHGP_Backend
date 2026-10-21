// ─── CandidatureResponseDTO.java ─────────────────────────────────────────────
package com.example.backend_sirhgp.dto.response;

import com.example.backend_sirhgp.model.Candidature;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CandidatureResponseDTO {

    private Long id;
    private Long offreId;
    private String offreTitre;

    private String nomCandidat;
    private String email;
    private String telephone;
    private String cvUrl;
    private String lettreMotivation;

    private Candidature.StatutCandidature statut;
    private LocalDateTime dateCandidature;
    private String commentaire;

    private Long employeId;
    private String employeName;

    // 4.3 — pré-évaluation & embauche
    private Long documentId;
    private Candidature.DecisionRecrutement decision;
    private java.math.BigDecimal noteEvaluation;
    private String avisEvaluation;
    private java.time.LocalDate dateEmbauche;
}
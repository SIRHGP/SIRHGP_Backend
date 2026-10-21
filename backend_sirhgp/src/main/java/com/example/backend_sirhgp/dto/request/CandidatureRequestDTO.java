// ─── CandidatureRequestDTO.java ──────────────────────────────────────────────
package com.example.backend_sirhgp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CandidatureRequestDTO {

    @NotNull(message = "L'offre est obligatoire")
    private Long offreId;

    @NotBlank(message = "Le nom du candidat est obligatoire")
    private String nomCandidat;

    @NotBlank(message = "L'email du candidat est obligatoire")
    private String email;

    private String telephone;
    private String cvUrl;
    private String lettreMotivation;
    private String commentaire;

    // Candidat interne (optionnel)
    private Long employeId;
}
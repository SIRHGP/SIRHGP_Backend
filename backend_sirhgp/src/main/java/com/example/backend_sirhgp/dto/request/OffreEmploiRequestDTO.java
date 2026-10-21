// ─── OffreEmploiRequestDTO.java ──────────────────────────────────────────────
package com.example.backend_sirhgp.dto.request;

import com.example.backend_sirhgp.model.OffreEmploi;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OffreEmploiRequestDTO {

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    private String description;

    @NotNull(message = "Le type de contrat est obligatoire")
    private OffreEmploi.TypeContrat typeContrat;

    private String lieu;
    private OffreEmploi.StatutOffre statut;
    private Long posteId;
    private Long departementId;

    /**
     * 5.9 bis — succursale d'origine de l'offre (filtre/tri pour l'ADMIN qui pilote
     * plusieurs agences). Null = siège ; forcé à la sienne pour un MANAGER scopé.
     */
    private Long sucursalId;
}
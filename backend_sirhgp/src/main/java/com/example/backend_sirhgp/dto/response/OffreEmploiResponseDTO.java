// ─── OffreEmploiResponseDTO.java ─────────────────────────────────────────────
package com.example.backend_sirhgp.dto.response;

import com.example.backend_sirhgp.model.OffreEmploi;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OffreEmploiResponseDTO {

    private Long id;
    private String titre;
    private String description;
    private OffreEmploi.TypeContrat typeContrat;
    private String lieu;
    private OffreEmploi.StatutOffre statut;

    private Long posteId;
    private String posteName;
    private Long departementId;
    private String departementNom;

    // 5.9 : rattachement sucursale — d'où vient l'offre (visibilité admin).
    private Long sucursalId;
    private String sucursalNom;

    private LocalDateTime datePublication;
    private LocalDateTime dateCloture;

    // Compteurs (enrichis par le service)
    private Long nbCandidatures;
}
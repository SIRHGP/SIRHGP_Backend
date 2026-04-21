package com.example.backend_sirhgp.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LigneFactureResponseDTO {

    private Long id;
    private String description;
    private Integer quantite;

    // CORRECTION : BigDecimal
    private BigDecimal prixUnitaire;
    private BigDecimal montantTotal;

    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    private Long factureId;

    // Tâche liée : résumé seulement
    private Long tacheId;
    private String tacheNom;
    private String tacheCode;
}
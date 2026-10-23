package com.example.backend_sirhgp.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Champs de saisie d'un document du cycle (étape 2 — Bon de commande,
 * étape 3 — Facture régulière). Même forme pour les deux étapes.
 * Les montants sont recalculés côté backend depuis les lignes (OHADA).
 */
@Data
public class FacturationDocumentRequestDTO {

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    private String description;

    @NotNull(message = "La date d'émission est obligatoire")
    private LocalDate dateEmission;

    /** Module comptable OHADA (500, 501, ...) ou AUTRE. */
    private String module;

    /** Taux de TVA global (Cameroun : 19,25 % par défaut). */
    private BigDecimal tauxTVA;

    /** Facturation interne (client du groupe) : non assujettie à la TVA. Null = déduction auto. */
    private Boolean assujettiTVA;

    /** Lignes de saisie (désignation, quantité, prix HT, compte produit). */
    @Valid
    private List<LigneFactureRequestDTO> lignesFacture;

    @DecimalMin(value = "0.0", message = "Le montant HT doit être positif ou nul")
    private BigDecimal montantHT;
}

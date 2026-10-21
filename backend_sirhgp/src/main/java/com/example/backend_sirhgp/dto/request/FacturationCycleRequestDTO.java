package com.example.backend_sirhgp.dto.request;

import com.example.backend_sirhgp.model.Facture;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Création d'un dossier de facturation (cycle 3 étapes).
 * Le projet est obligatoire : le BC et la facture finale sont des Factures
 * (FactureRequestDTO.exige projetId).
 */
@Data
public class FacturationCycleRequestDTO {

    @NotBlank(message = "Le titre du dossier est obligatoire")
    private String titre;

    private String description;

    @NotNull(message = "Le projet est obligatoire")
    private Long projetId;

    /** Client (compte employé) — optionnel (texte libre ailleurs). */
    private Long clientId;

    /** Sens du flux : VENTE (défaut) ou ACHAT. */
    private Facture.SensFacture sens;

    /** Fournisseur en texte libre (sens ACHAT). */
    private String nomFournisseur;

    private String commentaire;
}
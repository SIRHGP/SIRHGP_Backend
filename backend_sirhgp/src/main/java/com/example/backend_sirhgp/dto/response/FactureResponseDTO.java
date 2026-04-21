package com.example.backend_sirhgp.dto.response;

import com.example.backend_sirhgp.model.Facture;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * CORRECTION : Double → BigDecimal pour tous les montants.
 */
@Data
public class FactureResponseDTO {

    private Long id;
    private String numeroFacture;
    private String titre;
    private String description;
    private LocalDate dateEmission;
    private LocalDate dateEcheance;

    // CORRECTION : BigDecimal
    private BigDecimal montantHT;
    private BigDecimal montantTVA;
    private BigDecimal montantTTC;
    private BigDecimal montantPaye;
    private BigDecimal montantRestant;

    private Facture.StatutFacture statut;
    private Facture.TypeFacture typeFacture;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    // Références résumées
    private Long projetId;
    private String projetNom;
    private String projetCode;

    private ProjetResponseDTO.EmployerSummaryDTO emetteur;
    private ProjetResponseDTO.EmployerSummaryDTO client;

    private Set<LigneFactureResponseDTO> lignesFacture;

    // Indicateurs calculés
    private Double pourcentagePaye;
    private Boolean estPayee;
    private Boolean estEnRetard;
    private Long joursRestants;
    private Long delaiEmissionPaiement;
}
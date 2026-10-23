package com.example.backend_sirhgp.dto.response;

import com.example.backend_sirhgp.model.Facture;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FactureResponseDTO {

    private Long id;
    private String numeroFacture;
    private String titre;
    private String description;
    private LocalDate dateEmission;
    private LocalDate dateEcheance;

    private BigDecimal montantHT;
    private BigDecimal montantTVA;
    private BigDecimal montantTTC;
    private BigDecimal montantPaye;
    private BigDecimal montantRestant;

    private Facture.StatutFacture statut;
    private Facture.TypeFacture typeFacture;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    // Dispatch fields
    private String message;
    private boolean estConsultee;
    private LocalDateTime dateEnvoi;
    private LocalDateTime dateConsultation;

    // Références résumées
    private Long projetId;
    private String projetNom;
    private String projetCode;

    private EmployerSummaryDTO emetteur;
    private EmployerSummaryDTO client;

    private List<LigneFactureResponseDTO> lignesFacture;

    // Indicateurs calculés
    private Double pourcentagePaye;
    private Boolean estPayee;
    private Boolean estEnRetard;
    private Long joursRestants;
    private Long delaiEmissionPaiement;

    @Data
    public static class EmployerSummaryDTO {
        private Long id;
        private String name;
        private String email;
        private String posteName;
        private String departementNom;
    }
}

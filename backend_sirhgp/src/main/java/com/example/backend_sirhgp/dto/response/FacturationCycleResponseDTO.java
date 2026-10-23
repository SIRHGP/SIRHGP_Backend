package com.example.backend_sirhgp.dto.response;

import com.example.backend_sirhgp.model.Facture;
import com.example.backend_sirhgp.model.FacturationCycle;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dossier de facturation (cycle 3 étapes) — états d'avancement calculés
 * pour alimenter la barre de progression du modal.
 */
@Data
public class FacturationCycleResponseDTO {

    private Long id;
    private String numeroReference;
    private FacturationCycle.StatutCycle statut;

    private String titre;
    private String description;
    private Facture.SensFacture sens;
    private String nomFournisseur;
    private String commentaire;

    private Long clientId;
    private String clientNom;
    private Long projetId;
    private String projetNom;
    private String projetCode;

    // ─── Avancement du cycle (barre de progression 3 points) ─────────────────
    /** Étape 1 (proforma) complétée. */
    private boolean etape1Faite;
    /** Étape 2 (bon de commande) complétée. */
    private boolean etape2Faite;
    /** Étape 3 (facture régulière) complétée. */
    private boolean etape3Faite;
    /** Étape en cours : 1..3 ; 4 = cycle clôturé. */
    private int etapeCourante;
    /** Avancement 0 / 33 / 67 / 100. */
    private int avancementPct;
    private boolean cloturee;

    // ─── Étape 1 — proforma (PDF reçu) ────────────────────────────────────────
    private PieceProforma proforma;

    // ─── Étape 2 / 3 — documents générés / saisis ─────────────────────────────
    private DocResume bonCommande;
    private DocResume factureFinale;
    /** Étape 3 alternative : PDF reçu de la facture finale. */
    private PieceProforma pieceFinale;

    // ─── Totaux (issue de la facture régulière, sinon du BC) ─────────────────
    private BigDecimal montantTTC;
    private BigDecimal montantPaye;
    private BigDecimal montantRestant;

    // ─── Horodatage ───────────────────────────────────────────────────────────
    private LocalDateTime dateCreation;
    private LocalDateTime dateProforma;
    private LocalDateTime dateBonCommande;
    private LocalDateTime dateFactureFinale;
    private LocalDateTime dateCloture;

    /** PDF reçu et rattaché au dossier (étape 1 et 3). */
    @Data
    public static class PieceProforma {
        private Long documentId;
        private String nom;
        private Long taille;
        private LocalDateTime date;
    }

    /** Résumé d'une Facture du cycle (BC ou régulière). */
    @Data
    public static class DocResume {
        private Long id;
        private String numero;
        private String titre;
        private Facture.TypeFacture typeFacture;
        private Facture.StatutFacture statut;
        private LocalDate dateEmission;
        private BigDecimal montantHT;
        private BigDecimal montantTVA;
        private BigDecimal montantTTC;
        private BigDecimal montantPaye;
        private BigDecimal montantRestant;
        /** Vrai si un PDF est téléchargeable (documents générés côté serveur). */
        private boolean pdfDisponible;
        private List<LigneFactureResponseDTO> lignesFacture;
    }
}

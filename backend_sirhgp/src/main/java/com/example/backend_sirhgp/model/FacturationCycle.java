package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Dossier de facturation (refonte interface) : UNE facture = un cycle en 3 etapes
 * sequentielles — 1) PROFORMA (PDF recu) -> 2) BON DE COMMANDE (saisie + PDF genere)
 * -> 3) FACTURE REGULIERE (saisie ou piece recue apres paiement) -> CLOTURE.
 *
 * Regle fondamentale : une facture n'existe pleinement que lorsque les 3 etapes
 * sont completees. Les transitions sont controlees par FacturationCycleService.
 */
@Entity
@Table(
        name = "facturation_cycles",
        indexes = {
                @Index(name = "idx_facturation_cycle_statut", columnList = "statut"),
                @Index(name = "idx_facturation_cycle_client_id", columnList = "client_id"),
                @Index(name = "idx_facturation_cycle_numero_reference", columnList = "numero_reference", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacturationCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_reference", nullable = false, unique = true)
    private String numeroReference;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutCycle statut = StatutCycle.BROUILLON;

    /** Libelle du dossier (intitule de la prestation / du marche). */
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Sens du flux : VENTE (client) ou ACHAT (fournisseur). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Facture.SensFacture sens = Facture.SensFacture.VENTE;

    /** Fournisseur en texte libre (sens ACHAT). */
    private String nomFournisseur;

    // ─── Multi-tenant + rattachement projet ───────────────────────────────────
    /** Entreprise de rattachement (isolation multi-tenant). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    /**
     * Projet porteur du dossier — obligatoire car le BC et la facture finale
     * sont des Factures (FactureRequestDTO.exige projetId).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_id")
    private Projet projet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Employer client;

    // ─── Les 3 etapes du cycle ────────────────────────────────────────────────

    /** Etape 1 — PDF du proforma recu (piece de reference du cycle). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proforma_document_id")
    private Document proformaDocument;

    /** Etape 2 — Bon de commande genere depuis la saisie (Facture BON_COMMANDE). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bon_commande_facture_id")
    private Facture bonCommande;

    /** Etape 3 — Facture reguliere (Facture REGULIERE, chainer au BC). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_finale_id")
    private Facture factureFinale;

    /** Etape 3 (alternative) — piece PDF recue de la facture finale. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_finale_document_id")
    private Document factureFinaleDocument;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    // ─── Horodatage (tracabilite de chaque etape) ─────────────────────────────

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @Column(name = "date_proforma")
    private LocalDateTime dateProforma;

    @Column(name = "date_bc")
    private LocalDateTime dateBonCommande;

    @Column(name = "date_facture_finale")
    private LocalDateTime dateFactureFinale;

    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;

    public enum StatutCycle {
        BROUILLON,
        PROFORMA_RECU,
        BC_GENERE,
        BC_VALIDE,
        FACTURE_RECU,
        CLOTURE
    }

    public boolean isCloture() {
        return this.statut == StatutCycle.CLOTURE;
    }

    public boolean hasProforma() {
        return this.proformaDocument != null;
    }

    public boolean hasBonCommande() {
        return this.bonCommande != null;
    }

    public boolean hasFactureFinale() {
        return this.factureFinale != null || this.factureFinaleDocument != null;
    }

    /** Numero de l'etape en cours (1..3) ; 4 = cycle cloture. */
    public int etapeCourante() {
        if (isCloture()) return 4;
        if (hasFactureFinale()) return 3;
        if (hasBonCommande()) return 2;
        return 1;
    }
}

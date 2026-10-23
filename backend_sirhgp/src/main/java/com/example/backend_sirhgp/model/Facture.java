package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "factures", indexes = {
        @Index(name = "idx_facture_client_id", columnList = "client_id"),
        @Index(name = "idx_facture_emetteur_id", columnList = "emetteur_id"),
        @Index(name = "idx_facture_statut", columnList = "statut"),
        @Index(name = "idx_facture_est_consultee", columnList = "est_consultee")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"projet", "emetteur", "client", "entreprise", "lignesFacture"})
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(unique = true, nullable = false)
    private String numeroFacture;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDate dateEmission;

    private LocalDate dateEcheance;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montantHT;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal montantTVA = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal montantTTC = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal montantPaye = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal montantRestant = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutFacture statut = StatutFacture.BROUILLON;

    @Enumerated(EnumType.STRING)
    private TypeFacture typeFacture;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SensFacture sens = SensFacture.VENTE;

    private String module;
    private String nomFournisseur;
    private String nomDestinataire;
    private Long factureOrigineId;

    @Builder.Default
    private Boolean assujettiTVA = true;

    @Column(precision = 7, scale = 4)
    @Builder.Default
    private BigDecimal tauxTVA = new BigDecimal("0.1925");

    // ─── Dispatch (envoi au client) ───────────────────────────────────────────
    @Builder.Default
    private boolean estConsultee = false;

    @Column(name = "date_consultation")
    private LocalDateTime dateConsultation;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "date_envoi")
    private LocalDateTime dateEnvoi;

    // ─── Relations ────────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projet_id")
    private Projet projet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emetteur_id")
    private Employer emetteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Employer client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "ordre_ligne")
    @Builder.Default
    private List<LigneFacture> lignesFacture = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    private LocalDateTime dateModification;

    // ─── Enums ────────────────────────────────────────────────────────────────

    public enum StatutFacture {
        BROUILLON, EN_ATTENTE, PAYEE, PARTIELLEMENT_PAYEE, EN_RETARD, ANNULEE
    }

    public enum TypeFacture {
        ACOMPTE, SOLDE, AVOIR, REGULIERE, BON_COMMANDE, PROFORMA
    }

    public enum SensFacture {
        VENTE, ACHAT
    }

    // ─── Méthodes utilitaires ────────────────────────────────────────────────

    public void addLigneFacture(LigneFacture ligne) {
        this.lignesFacture.add(ligne);
        ligne.setFacture(this);
        calculerMontants();
    }

    public void removeLigneFacture(LigneFacture ligne) {
        this.lignesFacture.remove(ligne);
        ligne.setFacture(null);
        calculerMontants();
    }

    public void calculerMontants() {
        this.lignesFacture.forEach(LigneFacture::calculerMontant);
        this.montantHT = this.lignesFacture.stream()
                .map(LigneFacture::getMontantTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        if (!this.lignesFacture.isEmpty()) {
            BigDecimal taux = this.tauxTVA != null ? this.tauxTVA : BigDecimal.ZERO;
            BigDecimal facteurTVA = taux.compareTo(BigDecimal.ONE) > 0
                    ? taux.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP)
                    : taux;
            boolean taxable = !Boolean.FALSE.equals(this.assujettiTVA);
            this.lignesFacture.forEach(ligne -> {
                BigDecimal tvaLigne = taxable
                        ? ligne.getMontantTotal().multiply(facteurTVA).setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                ligne.setMontantTVA(tvaLigne);
            });
            this.montantTVA = this.lignesFacture.stream()
                    .map(LigneFacture::getMontantTVA)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
        } else if (this.montantTVA == null) {
            this.montantTVA = BigDecimal.ZERO;
        }
        this.montantTTC = this.montantHT.add(this.montantTVA).setScale(2, RoundingMode.HALF_UP);

        if (this.montantPaye == null) this.montantPaye = BigDecimal.ZERO;
        this.montantRestant = this.montantTTC.subtract(this.montantPaye).setScale(2, RoundingMode.HALF_UP);

        if (this.statut != StatutFacture.ANNULEE) {
            if (this.montantTTC.compareTo(BigDecimal.ZERO) > 0
                    && this.montantPaye.compareTo(this.montantTTC) >= 0) {
                this.statut = StatutFacture.PAYEE;
            } else if (this.montantPaye.compareTo(BigDecimal.ZERO) > 0) {
                this.statut = StatutFacture.PARTIELLEMENT_PAYEE;
            } else if (this.statut == StatutFacture.PAYEE
                    || this.statut == StatutFacture.PARTIELLEMENT_PAYEE) {
                this.statut = StatutFacture.BROUILLON;
            }
        }

        if (this.dateEcheance != null
                && this.dateEcheance.isBefore(LocalDate.now())
                && this.montantRestant.compareTo(BigDecimal.ZERO) > 0
                && this.statut != StatutFacture.PAYEE
                && this.statut != StatutFacture.ANNULEE) {
            this.statut = StatutFacture.EN_RETARD;
        }
    }

    public void marquerCommeConsultee() {
        this.estConsultee = true;
        this.dateConsultation = LocalDateTime.now();
    }

    public boolean estEnvoyee() {
        return this.client != null && this.dateEnvoi != null;
    }

    @PrePersist
    @PreUpdate
    public void prePersist() {
        calculerMontants();
        if (this.numeroFacture == null || this.numeroFacture.isBlank()) {
            String moisAnnee = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            this.numeroFacture = "FACT-" + moisAnnee + "-" + suffix;
        }
    }
}

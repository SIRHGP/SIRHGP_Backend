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
import java.util.HashSet;
import java.util.Set;
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
@ToString(exclude = {"projet", "emetteur", "client", "lignesFacture"})
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

    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<LigneFacture> lignesFacture = new HashSet<>();

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
        ACOMPTE, SOLDE, AVOIR, REGULIERE
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
        this.montantHT = this.lignesFacture.stream()
                .map(LigneFacture::getMontantTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        if (this.montantTVA == null) this.montantTVA = BigDecimal.ZERO;
        this.montantTTC = this.montantHT.add(this.montantTVA).setScale(2, RoundingMode.HALF_UP);

        if (this.montantPaye == null) this.montantPaye = BigDecimal.ZERO;
        this.montantRestant = this.montantTTC.subtract(this.montantPaye).setScale(2, RoundingMode.HALF_UP);

        int comparePaye = this.montantPaye.compareTo(this.montantTTC);
        if (comparePaye >= 0) {
            this.statut = StatutFacture.PAYEE;
        } else if (this.montantPaye.compareTo(BigDecimal.ZERO) > 0) {
            this.statut = StatutFacture.PARTIELLEMENT_PAYEE;
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
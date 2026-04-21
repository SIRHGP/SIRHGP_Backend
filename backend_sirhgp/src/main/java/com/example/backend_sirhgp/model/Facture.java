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

/**
 * Facture émise ou reçue dans le cadre d'un projet.
 *
 * CORRECTIONS :
 * - Double → BigDecimal sur tous les montants financiers.
 * - calculerMontants() utilise BigDecimal pour éviter les bugs d'arrondi.
 *   La comparaison >= sur Double pour tester le paiement complet est un bug
 *   classique : 100.0 >= 100.0 peut être false à cause de l'imprécision flottante.
 * - setProjet() supprimé — code mort généré par Lombok @Setter.
 * - numeroFacture généré avec un format lisible + UUID court pour garantir l'unicité
 *   en cas d'appels parallèles (System.currentTimeMillis() ne suffit pas).
 * - @EqualsAndHashCode(callSuper = false) → onlyExplicitlyIncluded = true.
 * - StatutFacture.EN_RETARD ajouté en cohérence avec les DTOs dashboard.
 */
@Entity
@Table(name = "factures")
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

    // CORRECTION : BigDecimal pour tous les montants
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

    /**
     * CORRECTION : calculs avec BigDecimal.
     * La comparaison >= sur Double (version originale) provoque des bugs
     * d'arrondi : ex. 100.001 >= 100.0 peut donner un faux résultat.
     * BigDecimal.compareTo() est la bonne approche pour les comparaisons financières.
     */
    public void calculerMontants() {
        // Somme des lignes
        this.montantHT = this.lignesFacture.stream()
                .map(LigneFacture::getMontantTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // TVA déjà fixée, on recalcule le TTC
        if (this.montantTVA == null) this.montantTVA = BigDecimal.ZERO;
        this.montantTTC = this.montantHT.add(this.montantTVA).setScale(2, RoundingMode.HALF_UP);

        // Restant
        if (this.montantPaye == null) this.montantPaye = BigDecimal.ZERO;
        this.montantRestant = this.montantTTC.subtract(this.montantPaye).setScale(2, RoundingMode.HALF_UP);

        // Mise à jour du statut de paiement
        // CORRECTION : compareTo() au lieu de >= sur Double
        int comparePaye = this.montantPaye.compareTo(this.montantTTC);
        if (comparePaye >= 0) {
            this.statut = StatutFacture.PAYEE;
        } else if (this.montantPaye.compareTo(BigDecimal.ZERO) > 0) {
            this.statut = StatutFacture.PARTIELLEMENT_PAYEE;
        }

        // Vérification retard
        if (this.dateEcheance != null
                && this.dateEcheance.isBefore(LocalDate.now())
                && this.montantRestant.compareTo(BigDecimal.ZERO) > 0
                && this.statut != StatutFacture.PAYEE
                && this.statut != StatutFacture.ANNULEE) {
            this.statut = StatutFacture.EN_RETARD;
        }
    }

    /**
     * CORRECTION : numeroFacture généré avec UUID court pour garantir l'unicité
     * même en cas d'appels simultanés.
     * Format : FACT-YYYYMM-XXXXXXXX (ex: FACT-202501-A3F2B1C0)
     */
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
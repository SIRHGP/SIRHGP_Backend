// ─── Evaluation.java ─────────────────────────────────────────────────────────
package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Évaluation d'un employé (2.4 — Module Évaluations, refonte 6.1).
 *
 * <p>Deux axes distincts (formulaire à deux champs) :
 * <ul>
 *   <li><b>type</b> (fréquence) : ANNUELLE / TRIMESTRIELLE / A_DATE
 *       — remplace les anciens PERIODIQUE / PROJET / OBJECTIF (migration
 *       V20261007_1) ;</li>
 *   <li><b>categorie</b> (objet / décision) : PROMOTION / RENVOI / RECADREMENT.</li>
 * </ul>
 *
 * <p>Workflow : BROUILLON → SOUMISE → VALIDEE.
 * LEAD crée (BROUILLON) puis soumet → MANAGER valide ; les évaluations
 * écrites par un MANAGER ou un ADMIN sont VALIDÉE directement à la création.
 * Le <b>rapport</b> (récap auto) est figé à la validation dans {@code rapport}.
 */
@Entity
@Table(name = "evaluations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"evalue", "evaluateur", "entreprise", "criteres"})
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TypeEvaluation type = TypeEvaluation.ANNUELLE;

    /** Catégorie / objet de l'évaluation (refonte 6.1) — obligatoire à la création. */
    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", nullable = false)
    private CategorieEvaluation categorie;

    /** Objet libre de l'évaluation (ex. « Promotion au poste de chef de service »). */
    @Column(length = 255)
    private String objet;

    private Integer annee;
    private String periode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutEvaluation statut = StatutEvaluation.BROUILLON;

    @Column(precision = 4, scale = 2)
    private BigDecimal noteGlobale;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    /** Rapport figé à la validation : récap KPI + critères + note + décision (6.1). */
    @Column(columnDefinition = "TEXT")
    private String rapport;

    private LocalDate dateRapport;

    private LocalDate dateEvaluation;

    // 360° : plusieurs évaluateurs possibles par évalué
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evalue_id", nullable = false)
    private Employer evalue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluateur_id", nullable = false)
    private Employer evaluateur;

    // Multi-tenant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CritereEvaluation> criteres = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    private LocalDateTime dateModification;

    // ─── Enums ────────────────────────────────────────────────────────────────
    /** Fréquence de l'évaluation (6.1 — anciennes valeurs remplacées). */
    public enum TypeEvaluation { ANNUELLE, TRIMESTRIELLE, A_DATE }
    /** Catégorie / décision de l'évaluation (6.1). */
    public enum CategorieEvaluation { PROMOTION, RENVOI, RECADREMENT }
    public enum StatutEvaluation { BROUILLON, SOUMISE, VALIDEE }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    public void addCritere(CritereEvaluation c) {
        criteres.add(c);
        c.setEvaluation(this);
    }

    public void removeCritere(CritereEvaluation c) {
        criteres.remove(c);
        c.setEvaluation(null);
    }

    /** Note = moyenne pondérée des critères (sur 20). */
    public BigDecimal calculerMoyenne() {
        if (criteres.isEmpty()) return null;
        BigDecimal totalCoef = BigDecimal.ZERO;
        BigDecimal totalPondere = BigDecimal.ZERO;
        for (CritereEvaluation c : criteres) {
            if (c.getNote() == null) {
                throw new IllegalArgumentException(
                        "Note manquante pour le critère « " + c.getLibelle() + " »");
            }
            BigDecimal coef = c.getCoefficient() != null ? c.getCoefficient() : BigDecimal.ONE;
            totalPondere = totalPondere.add(c.getNote().multiply(coef));
            totalCoef = totalCoef.add(coef);
        }
        this.noteGlobale = totalPondere.divide(totalCoef, 2, RoundingMode.HALF_UP);
        return this.noteGlobale;
    }
}
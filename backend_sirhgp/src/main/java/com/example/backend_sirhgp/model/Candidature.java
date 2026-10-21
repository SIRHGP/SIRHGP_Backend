// ─── Candidature.java ────────────────────────────────────────────────────────
package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Candidature à une offre d'emploi (2.2 — Module Recrutement).
 * Le candidat peut être externe (nom/email libres) ou un employé existant (employeId).
 * Pipeline (simplifié 5.13) : NOUVELLE → ENTRETIEN → EVALUATION → RECRUTE / REFUSEE.
 */
@Entity
@Table(name = "candidatures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"offre", "entreprise"})
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offre_id", nullable = false)
    private OffreEmploi offre;

    @Column(nullable = false)
    private String nomCandidat;

    @Column(nullable = false)
    private String email;

    private String telephone;
    private String cvUrl;

    @Column(columnDefinition = "TEXT")
    private String lettreMotivation;

    // Lien vers le document CV téléversé (optionnel, 4.3) — en plus de cvUrl externe
    private Long documentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutCandidature statut = StatutCandidature.NOUVELLE;

    // ─── Pré-évaluation & pipeline (4.3) ─────────────────────
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DecisionRecrutement decision = DecisionRecrutement.PENDING;

    @Column(precision = 4, scale = 2)
    private java.math.BigDecimal noteEvaluation; // note pré-évaluation /20

    @Column(columnDefinition = "TEXT")
    private String avisEvaluation;

    private LocalDate dateEmbauche; // rempli à l'embauche (candidat → employé)

    // Candidat interne : employé existant (optionnel)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id")
    private Employer employe;

    private LocalDateTime dateCandidature;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    // Multi-tenant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    private LocalDateTime dateModification;

    // ─── Enums ────────────────────────────────────────────────────────────────
    public enum StatutCandidature {
        NOUVELLE, ENTRETIEN, EVALUATION, RECRUTE, REFUSEE,
        /** 5.13 — anciens statuts conservés en lecture seule (suppression stricte) :
         * aucune transition n'y mène ; seules les sorties manuelles vers le
         * pipeline simplifié (ou REFUSEE) sont autorisées, en attendant la
         * migration SQL qui restreindra l'ENUM une fois ces dossiers déplacés. */
        @Deprecated PRESELECTION, @Deprecated TEST, @Deprecated OFFRE
    }

    /** Verdict de la pré-évaluation du dossier candidat (4.3). */
    public enum DecisionRecrutement {
        PENDING, A_EVALUER, EN_STAGE, ENGAGE, REFUSE_AVANT
    }
}
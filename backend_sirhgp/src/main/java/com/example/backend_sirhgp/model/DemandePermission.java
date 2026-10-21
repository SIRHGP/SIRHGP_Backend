package com.example.backend_sirhgp.model;

// ─── DemandePermission.java (refonte 5.6 — module Timesheet) ─────────────────
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Demande de permission (absence) soumise depuis le Timesheet.
 *
 * Chaîne de validation par rôle (refonte 5.6) :
 *   STAFF / STAGIAIRE → chef de département (superviseur LEAD de 5.5)
 *   LEAD              → MANAGER de l'entreprise
 *   MANAGER           → ADMIN de l'entreprise
 *   ADMIN / SUPER_ADMIN → sans valideur dédié (traitées par l'ADMIN du scope)
 *
 * Le valideur est SNAPSHOTTÉ à la création (valideurId / valideurNom) :
 * si le chef de département change ensuite, l'historique des demandes en
 * attente reste attribué à la bonne personne. Un valideurId null = demande
 * orpheline → visible et traitable par tout MANAGER/ADMIN du scope.
 *
 * Le justificatif est un Document existant (documentId) téléversé via
 * /api/documents/upload — aucune duplication de stockage.
 *
 * CORRECTIONS de style (cf. Employer.java) : pas de @Data (entité JPA),
 * equals/hashCode sur l'id uniquement, toString sans relations lazy.
 */
@Entity
@Table(name = "demandes_permission", indexes = {
        @Index(name = "idx_perm_demandeur", columnList = "demandeur_id"),
        @Index(name = "idx_perm_valideur", columnList = "valideur_id"),
        @Index(name = "idx_perm_statut", columnList = "statut")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"demandeur", "entreprise"})
public class DemandePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypePermission type;

    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate dateFin;

    // Renseignées uniquement pour SORTIE (demi-journée / quelques heures)
    private LocalTime heureDebut;
    private LocalTime heureFin;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutPermission statut = StatutPermission.EN_ATTENTE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demandeur_id", nullable = false)
    private Employer demandeur;

    /** Valideur snapshoté à la création (null = demande orpheline → ADMIN/MANAGER). */
    @Column(name = "valideur_id")
    private Long valideurId;
    @Column(name = "valideur_nom")
    private String valideurNom;

    @Column(columnDefinition = "TEXT")
    private String commentaireValideur;

    /** Justificatif : référence d'un Document téléversé via le module Documents. */
    @Column(name = "justificatif_id")
    private Long justificatifId;
    @Column(name = "justificatif_nom")
    private String justificatifNom;

    // Multi-tenant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    private LocalDateTime dateDecision;

    // ─── Enums ────────────────────────────────────────────────────────────────
    public enum TypePermission { SORTIE, ABSENCE_JOURNEE, CONGE_ANNUEL, CONGE_MALADIE, PERMISSION_SANS_SOLDE, AUTRE }
    public enum StatutPermission { EN_ATTENTE, APPROUVEE, REJETEE, ANNULEE }
}

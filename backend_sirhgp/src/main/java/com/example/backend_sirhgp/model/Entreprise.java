// ─── Entreprise.java ─────────────────────────────────────────────────────────
package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entreprise (multi-tenant).
 *
 * Chaque donnée métier (Employer, Departement, Poste, Projet, Tache, Facture,
 * Document, Evenement) référence l'entreprise à laquelle elle appartient.
 *
 * Le rôle SUPER_ADMIN est transverse : il peut voir et gérer toutes les entreprises.
 * Les autres rôles (ADMIN, MANAGER, STAFF) sont scopés à leur propre entreprise.
 *
 * Les données existantes créées avant ce système sont rattachées à
 * l'"Entreprise par défaut" lors de l'initialisation (voir ApplicationInitializer).
 */
@Entity
@Table(name = "entreprises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {})
public class Entreprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(unique = true, nullable = false)
    @EqualsAndHashCode.Include
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String secteurActivite;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TypeEntreprise type = TypeEntreprise.AUTRE;

    // ─── Configuration pointage (4.2, par entreprise, modifiable par les admins) ──
    // Heure d'arrivée standard (au-delà = retard). Nullable : pas de contrôle.
    private java.time.LocalTime heureArriveeStandard;
    // Heure de départ standard (avant = départ anticipé). Nullable.
    private java.time.LocalTime heureDepartStandard;
    // Heure d'arrivée minimale (avant = pointage rédhibitoire/anormal). Nullable.
    private java.time.LocalTime heureArriveeMinimale;

    private String adresse;
    private String ville;
    private String telephone;
    private String email;
    private String logoUrl;

    // ─── Mentions légales du bulletin de paie (5.1) ──
    /** Registre de commerce (RCCM). */
    private String rccm;
    /** Numéro d'employeur CNPS. */
    @Column(name = "numero_cnps_employeur")
    private String numeroCnpsEmployeur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutEntreprise statut = StatutEntreprise.ACTIVE;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    private LocalDateTime dateModification;

    // ─── Enum ─────────────────────────────────────────────────────────────────

    public enum StatutEntreprise {
        ACTIVE, SUSPENDUE
    }

    /**
     * Type d'entreprise (catégorie juridique/organisationnelle — régime camerounais/OHADA).
     * PME / Grande entreprise / Startup / ONG / Administration / Autre.
     */
    public enum TypeEntreprise {
        PME, GRANDE_ENTREPRISE, STARTUP, ONG, ADMINISTRATION, AUTRE
    }
}
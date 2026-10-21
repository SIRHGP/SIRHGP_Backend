// ─── OffreEmploi.java ────────────────────────────────────────────────────────
package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Offre d'emploi (2.2 — Module Recrutement).
 * Multi-tenant : rattachée à une entreprise.
 */
@Entity
@Table(name = "offres_emploi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"poste", "departement", "entreprise", "sucursal"})
public class OffreEmploi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TypeContrat typeContrat = TypeContrat.CDI;

    private String lieu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutOffre statut = StatutOffre.OUVERTE;

    // Poste à pourvoir (optionnel)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_id")
    private Poste poste;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departement_id")
    private Departement departement;

    // Multi-tenant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    // 5.9 : sucursal de rattachement (nullable — siège / données héritées).
    // Posée automatiquement à la création, d'après la succursale du créateur.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime datePublication;

    private LocalDateTime dateCloture;

    @UpdateTimestamp
    private LocalDateTime dateModification;

    // ─── Enums ────────────────────────────────────────────────────────────────
    public enum TypeContrat { CDI, CDD, STAGE, FREELANCE, TEMPS_PARTIEL }
    public enum StatutOffre { OUVERTE, SUSPENDUE, FERMEE }
}
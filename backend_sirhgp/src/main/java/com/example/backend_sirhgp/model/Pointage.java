// ─── Pointage.java ────────────────────────────────────────────────────────────
package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pointage (badgeuse) — 4.1 : refonte du Timesheet.
 *
 * Principe simple : un clic à l'arrivée (le matin), un clic au départ (le soir) —
 * la durée est calculée automatiquement (départ − arrivée). 5 jours sur 7
 * (lundi → vendredi) : la pointeuse refuse samedi et dimanche.
 * Un seul pointage par jour et par employé (arrivée = 1er clic, départ = 2e clic).
 * Aucune imputation de projet — le pointage est global à la journée.
 */
@Entity
@Table(name = "pointages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"employe", "entreprise"})
public class Pointage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalDateTime arrivee;

    @Column
    private LocalDateTime depart; // nullable = départ pas encore pointé

    /** Vrai si l'heure de départ a été saisie manuellement (rattrapage d'oubli). */
    @Builder.Default
    private boolean corrige = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employe_id", nullable = false)
    private Employer employe;

    // Multi-tenant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    private LocalDateTime dateModification;
}
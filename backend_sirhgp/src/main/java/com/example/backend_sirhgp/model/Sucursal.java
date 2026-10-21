// ─── Sucursal.java ────────────────────────────────────────────────────────────
package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Sucursal (établissement secondaire — régime OHADA/Cameroun).
 *
 * Hiérarchie multi-tenant à 3 niveaux :
 * SUPER_ADMIN (transversal) → ENTREPRISE (admin général) → SUCURSALE (admin scopé).
 *
 * Un compte ADMIN rattaché à une sucursal ne voit que les données de sa sucursal ;
 * un ADMIN sans sucursal voit toute son entreprise (3.4).
 */
@Entity
@Table(name = "sucursales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"entreprise", "manager"})
public class Sucursal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name;

    private String ville;
    private String adresse;
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutSucursal statut = StatutSucursal.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    // 5.9 : manager de la sucursal (FK inverse — affectation faite par l'ADMIN de l'entreprise).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employer manager;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    private LocalDateTime dateModification;

    // ─── Enum ─────────────────────────────────────────────────────────────────
    public enum StatutSucursal { ACTIVE, SUSPENDUE }
}
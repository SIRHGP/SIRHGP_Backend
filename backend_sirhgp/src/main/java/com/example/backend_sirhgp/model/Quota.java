package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Refonte 5.9 (§5) — quotas de la plateforme.
 * entreprise_id NULL = quota global par défaut appliqué à tous les tenants.
 * Valeurs null = illimité.
 */
@Entity
@Table(name = "quotas", indexes = @Index(name = "idx_quota_entreprise", columnList = "entreprise_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Quota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** null = quota global par défaut. */
    @Column(name = "entreprise_id")
    private Long entrepriseId;

    private Integer maxEmployes;
    private Integer maxSucursales;
    private Long maxStockageMo;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    private LocalDateTime dateModification;
}
package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Refonte 5.9 (§5) — override d'un feature flag pour une entreprise donnée. */
@Entity
@Table(name = "feature_flag_overrides",
        uniqueConstraints = @UniqueConstraint(name = "uk_flag_entreprise", columnNames = {"flag_id", "entreprise_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FeatureFlagOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flag_id", nullable = false)
    private FeatureFlag flag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @Column(nullable = false)
    private boolean actif;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;
}
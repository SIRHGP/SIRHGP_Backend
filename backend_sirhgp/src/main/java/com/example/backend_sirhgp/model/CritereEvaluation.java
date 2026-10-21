// ─── CritereEvaluation.java ──────────────────────────────────────────────────
package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Critère d'évaluation (2.4 — Module Évaluations).
 * Porté par une évaluation ; note sur 20, pondéré par un coefficient.
 */
@Entity
@Table(name = "criteres_evaluation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "evaluation")
public class CritereEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal note; // sur 20

    @Column(nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal coefficient = BigDecimal.ONE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;
}
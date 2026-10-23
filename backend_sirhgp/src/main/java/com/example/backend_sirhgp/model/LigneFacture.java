package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * CORRECTION : Double → BigDecimal sur prixUnitaire et montantTotal.
 * C'est la cause directe de l'erreur dans Facture.calculerMontants() :
 * le stream était Stream<Double> alors que reduce attendait BigDecimal.ZERO.
 * En passant getMontantTotal() en BigDecimal, le stream devient Stream<BigDecimal>
 * et reduce(BigDecimal.ZERO, BigDecimal::add) fonctionne correctement.
 */
@Entity
@Table(name = "lignes_facture")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"facture", "tache"})
public class LigneFacture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Integer quantite;

    // CORRECTION : BigDecimal
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal montantTotal = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal montantTVA = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id", nullable = false)
    private Facture facture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tache_id")
    private Tache tache;

    @PrePersist
    @PreUpdate
    public void calculerMontant() {
        if (quantite != null && prixUnitaire != null) {
            this.montantTotal = prixUnitaire
                    .multiply(BigDecimal.valueOf(quantite))
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }
}

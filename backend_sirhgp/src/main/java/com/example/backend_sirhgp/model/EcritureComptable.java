package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Écriture comptable générée automatiquement (SYSCOHADA) :
 * - validation d'une facture de vente : D 411 Clients / C 701-706-707 Produits / C 4431 TVA collectée ;
 * - encaissement d'un règlement : D 512/571 Trésorerie / C 411 Clients.
 */
@Entity
@Table(name = "ecritures_comptables", indexes = {
        @Index(name = "idx_ecriture_facture_id", columnList = "facture_id"),
        @Index(name = "idx_ecriture_compte", columnList = "compte")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcritureComptable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id")
    private Facture facture;

    /** Journal comptable : VENTE (facture) ou TRESORERIE (règlement). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Journal journal;

    public enum Journal { VENTE, TRESORERIE }

    @Column(nullable = false)
    private LocalDate date;

    /** Numéro de compte SYSCOHADA : 411, 701, 706, 707, 4431, 512, 571… */
    @Column(nullable = false, length = 10)
    private String compte;

    @Column(nullable = false)
    private String libelle;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal credit = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;
}

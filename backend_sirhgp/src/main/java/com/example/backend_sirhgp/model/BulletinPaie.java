// ─── BulletinPaie.java ───────────────────────────────────────────────────────
package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bulletin de paie (standard camerounais).
 *
 * Calcul (moteur PaieService) :
 * - BRUT = salaireBase + heuresSupp + primes + indemnités
 * - Cotisations salariales : CNPS (4,4%), IRPP (barème progressif), PVID, CFC
 * - Cotisation patronale : CNPS patronale (5,05%), TPR (1%)
 * - NET À PAYER = BRUT − retenues salariales
 */
@Entity
@Table(name = "bulletins_paie", indexes = {
        @Index(name = "idx_bulletin_employe", columnList = "employe_id"),
        @Index(name = "idx_bulletin_periode", columnList = "mois,annee"),
        @Index(name = "idx_bulletin_entreprise", columnList = "entreprise_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"employe", "entreprise"})
public class BulletinPaie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    private Employer employe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    // ─── Période ─────────────────────────────────────────────
    @Column(nullable = false)
    private Integer mois;   // 1..12
    @Column(nullable = false)
    private Integer annee;

    // ─── Éléments du brut ────────────────────────────────────
    @Column(precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal salaireBase = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal heuresTravaillees = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal heuresSupp = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal montantHeuresSupp = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal primes = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal indemnites = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal brut = BigDecimal.ZERO;

    // ─── Cotisations & retenues (salariales + patronales) ────
    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cnpsSalariale = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cnpsPatronale = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal irpp = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal pvid = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal cfc = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal tpr = BigDecimal.ZERO;

    /** 5.1 — Retenues diverses (absences non justifiées + retards), calculées depuis les pointages. */
    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal retenuesDiverses = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalRetenues = BigDecimal.ZERO;

    // ─── Net ─────────────────────────────────────────────────
    @Column(precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal netAPayer = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal coutTotalEmployeur = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;
}
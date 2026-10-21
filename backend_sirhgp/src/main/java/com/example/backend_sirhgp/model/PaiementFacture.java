package com.example.backend_sirhgp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Règlement d'une facture (journal de trésorerie OHADA).
 * Écriture associée : débit 512 Banque / 571 Caisse — crédit 411 Clients.
 */
@Entity
@Table(name = "paiements_facture", indexes = @Index(name = "idx_paiement_facture_id", columnList = "facture_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaiementFacture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facture_id", nullable = false)
    private Facture facture;

    @Column(nullable = false)
    private LocalDate datePaiement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModePaiement mode;

    /** Mode de paiement SYSCOHADA : 571 Caisse, 512 Banque, 521 Avances, 532 Chèques. */
    public enum ModePaiement {
        ESPECES("571"), CHEQUE("532"), VIREMENT("512"), MOBILE_MONEY("512"), AUTRE("571");
        private final String compteComptable;
        ModePaiement(String c) { this.compteComptable = c; }
        public String getCompteComptable() { return compteComptable; }
    }

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    /** Référence du règlement (n° de chèque, référence virement…). */
    private String reference;

    private String note;

    /** Compte comptable de trésorerie débité (571/512…). */
    @Column(length = 10)
    private String compteComptable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enregistre_par_id")
    private Employer enregistrePar;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dateCreation;
}

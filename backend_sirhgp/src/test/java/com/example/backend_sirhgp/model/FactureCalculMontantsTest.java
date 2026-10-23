package com.example.backend_sirhgp.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FactureCalculMontantsTest {

    @Test
    void recalculatesLineAmountsBeforeSumming() {
        LigneFacture line = LigneFacture.builder()
                .description("Service")
                .quantite(3)
                .prixUnitaire(new BigDecimal("125.50"))
                .montantTotal(BigDecimal.ZERO)
                .build();
        Facture facture = factureAvec(line);

        facture.calculerMontants();

        assertEquals(new BigDecimal("376.50"), facture.getMontantHT());
        assertEquals(new BigDecimal("376.50"), line.getMontantTotal());
    }

    @Test
    void emptyInvoiceCannotBeMarkedPaid() {
        Facture facture = factureAvec();
        facture.setStatut(Facture.StatutFacture.PAYEE);

        facture.calculerMontants();

        assertEquals(Facture.StatutFacture.BROUILLON, facture.getStatut());
    }

    @Test
    void preservesCancelledStatus() {
        Facture facture = factureAvec();
        facture.setStatut(Facture.StatutFacture.ANNULEE);

        facture.calculerMontants();

        assertEquals(Facture.StatutFacture.ANNULEE, facture.getStatut());
    }

    @Test
    void calculatesLineAndHeaderVatFromTheConfiguredRate() {
        Facture facture = factureAvec(LigneFacture.builder()
                .description("Service")
                .quantite(1)
                .prixUnitaire(new BigDecimal("100.00"))
                .build());
        facture.setTauxTVA(new BigDecimal("19.25"));
        facture.setAssujettiTVA(true);

        facture.calculerMontants();

        assertEquals(new BigDecimal("19.25"), facture.getMontantTVA());
        assertEquals(new BigDecimal("19.25"), facture.getLignesFacture().get(0).getMontantTVA());
        assertEquals(new BigDecimal("119.25"), facture.getMontantTTC());
    }

    @Test
    void doesNotChargeVatWhenInvoiceIsExempt() {
        Facture facture = factureAvec(LigneFacture.builder()
                .description("Service interne")
                .quantite(1)
                .prixUnitaire(new BigDecimal("100.00"))
                .montantTVA(new BigDecimal("19.25"))
                .build());
        facture.setTauxTVA(new BigDecimal("19.25"));
        facture.setAssujettiTVA(false);

        facture.calculerMontants();

        assertEquals(new BigDecimal("0.00"), facture.getMontantTVA());
        assertEquals(new BigDecimal("0.00"), facture.getLignesFacture().get(0).getMontantTVA());
        assertEquals(new BigDecimal("100.00"), facture.getMontantTTC());
    }

    @Test
    void preservesExplicitVatForInvoicesWithoutLines() {
        Facture facture = factureAvec();
        facture.setMontantHT(new BigDecimal("100.00"));
        facture.setMontantTVA(new BigDecimal("19.25"));

        facture.calculerMontants();

        assertEquals(new BigDecimal("19.25"), facture.getMontantTVA());
        assertEquals(new BigDecimal("19.25"), facture.getMontantTTC());
    }

    private Facture factureAvec(LigneFacture... lignes) {
        Facture facture = Facture.builder()
                .montantTVA(BigDecimal.ZERO)
                .montantPaye(BigDecimal.ZERO)
                .statut(Facture.StatutFacture.BROUILLON)
                .dateEcheance(LocalDate.now().plusDays(5))
                .build();
        facture.setLignesFacture(List.of(lignes));
        return facture;
    }
}

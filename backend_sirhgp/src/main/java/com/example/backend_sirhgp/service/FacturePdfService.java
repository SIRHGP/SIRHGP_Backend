// ─── FacturePdfService.java ──────────────────────────────────────────────────
package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.model.Entreprise;
import com.example.backend_sirhgp.model.Facture;
import com.example.backend_sirhgp.model.LigneFacture;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Génération PDF d'un document du cycle de facturation (OpenPDF) —
 * mêmes mentions OHADA/SYSCOHADA que le bulletin de paie (PaiePdfService) :
 * en-tête entreprise (RCCM, N° CNPS), lignes, TVA, totaux, montant en lettres.
 * Utilisé pour le Bon de commande (étape 2) et la Facture régulière (étape 3).
 */
@Slf4j
@Service
public class FacturePdfService {

    public byte[] generer(Facture f) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            writeHeader(doc, f, f.getEntreprise());
            writeIdentification(doc, f);
            writeLignes(doc, f);
            writeTotaux(doc, f);
            writePied(doc, f);

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Erreur génération PDF facture : " + e.getMessage(), e);
        }
    }

    // ── En-tête : entreprise émettrice + titre du document ────────────────────
    private void writeHeader(Document doc, Facture f, Entreprise ent) throws DocumentException {
        PdfPTable head = new PdfPTable(2);
        head.setWidthPercentage(100);

        PdfPCell cEnt = new PdfPCell();
        cEnt.setBorder(PdfPCell.NO_BORDER);
        cEnt.addElement(new Paragraph(ent != null ? ent.getName() : "Entreprise",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        if (ent != null) {
            if (ent.getRccm() != null)
                cEnt.addElement(new Paragraph("RCCM : " + ent.getRccm(), FontFactory.getFont(FontFactory.HELVETICA, 9)));
            if (ent.getNumeroCnpsEmployeur() != null)
                cEnt.addElement(new Paragraph("N° employeur CNPS : " + ent.getNumeroCnpsEmployeur(),
                        FontFactory.getFont(FontFactory.HELVETICA, 9)));
            String addr = (ent.getAdresse() != null ? ent.getAdresse() + " " : "")
                    + (ent.getVille() != null ? ent.getVille() : "");
            if (!addr.isBlank())
                cEnt.addElement(new Paragraph(addr.trim(), FontFactory.getFont(FontFactory.HELVETICA, 9)));
        }

        PdfPCell cTitre = new PdfPCell();
        cTitre.setBorder(PdfPCell.NO_BORDER);
        cTitre.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cTitre.addElement(new Paragraph(titreDocument(f), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
        cTitre.addElement(new Paragraph("N° " + f.getNumeroFacture(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        if (f.getDateEmission() != null)
            cTitre.addElement(new Paragraph("Émission : " + f.getDateEmission(),
                    FontFactory.getFont(FontFactory.HELVETICA, 10)));

        head.addCell(cEnt);
        head.addCell(cTitre);
        doc.add(head);
        doc.add(new Paragraph(" "));
    }

    // ── Identification : client / fournisseur / projet / objet ────────────────
    private void writeIdentification(Document doc, Facture f) throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);

        boolean achat = f.getSens() == Facture.SensFacture.ACHAT;
        String valeurA = achat
                ? nvl(f.getNomFournisseur())
                : (f.getClient() != null ? f.getClient().getName() : nvl(f.getNomDestinataire()));
        addInfoRow(t, achat ? "Fournisseur" : "Client", valeurA);
        addInfoRow(t, "Projet", f.getProjet() != null
                ? (f.getProjet().getCodeProjet() != null ? f.getProjet().getCodeProjet() + " — " : "") + f.getProjet().getNom()
                : "—");
        if (f.getDescription() != null && !f.getDescription().isBlank()) {
            addInfoRow(t, "Objet", f.getDescription());
        }
        doc.add(t);
        doc.add(new Paragraph(" "));
    }

    // ── Lignes du document (désignation / qté / PU / HT / TVA / TTC) ──────────
    private void writeLignes(Document doc, Facture f) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{4f, 1f, 2f, 2.2f, 2f, 2.2f});
        table.setWidthPercentage(100);

        Font fh = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        for (String h : new String[]{"Désignation", "Qté", "PU HT", "Montant HT", "TVA", "Total TTC"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, fh));
            c.setBackgroundColor(new java.awt.Color(0xF1, 0xF5, 0xF9));
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setPadding(5);
            table.addCell(c);
        }

        List<LigneFacture> lignes = new ArrayList<>(f.getLignesFacture());

        Font fb = FontFactory.getFont(FontFactory.HELVETICA, 9);
        if (lignes.isEmpty()) {
            // Repli : facture mono-montant (ancien formulaire) — une ligne unique.
            addLigne(table, fb, f.getTitre(), 1, f.getMontantHT(),
                    f.getMontantHT(), f.getMontantTVA(), f.getMontantTTC());
        } else {
            for (LigneFacture l : lignes) {
                BigDecimal ht = safe(l.getMontantTotal());
                BigDecimal ttc = ht.add(safe(l.getMontantTVA()));
                addLigne(table, fb, l.getDescription(), l.getQuantite(), l.getPrixUnitaire(),
                        ht, l.getMontantTVA(), ttc);
            }
        }
        doc.add(table);
    }

    // ── Totaux HT / TVA / TTC + montant en lettres ────────────────────────────
    private void writeTotaux(Document doc, Facture f) throws DocumentException {
        doc.add(new Paragraph(" "));
        PdfPTable tot = new PdfPTable(2);
        tot.setWidthPercentage(50);
        tot.setHorizontalAlignment(Element.ALIGN_RIGHT);

        boolean assujetti = !Boolean.FALSE.equals(f.getAssujettiTVA());
        addRow(tot, "Total HT", f.getMontantHT(), false);
        if (assujetti) {
            String taux = f.getTauxTVA() != null ? f.getTauxTVA().toPlainString() : "19.25";
            addRow(tot, "Total TVA (" + taux + " %)", f.getMontantTVA(), false);
        } else {
            addRow(tot, "TVA : non applicable (facturation interne)", BigDecimal.ZERO, false);
        }
        addRow(tot, "TOTAL TTC", f.getMontantTTC(), true);
        doc.add(tot);

        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("Arrêté le présent document à la somme de : "
                        + PaiePdfService.enLettres(f.getMontantTTC()) + " francs CFA.",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
    }

    // ── Pied : mentions SYSCOHADA ─────────────────────────────────────────────
    private void writePied(Document doc, Facture f) throws DocumentException {
        doc.add(new Paragraph(" "));
        Font fm = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8);
        doc.add(new Paragraph("Document conforme au référentiel SYSCOHADA révisé — Comptes 411 Clients / 701-706-707 Produits"
                        + (!Boolean.FALSE.equals(f.getAssujettiTVA()) ? " / 4431 TVA collectée" : "") + ".",
                fm));
        if (f.getTypeFacture() == Facture.TypeFacture.BON_COMMANDE) {
            doc.add(new Paragraph("Bon de commande : document de proposition et de validation — il sera régularisé "
                    + "par la facture régulière après paiement.", fm));
        }
        if (f.getTypeFacture() == Facture.TypeFacture.PROFORMA) {
            doc.add(new Paragraph("Proforma : document sans valeur comptable — aucune écriture OHADA générée.", fm));
        }
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private String titreDocument(Facture f) {
        if (f.getTypeFacture() == Facture.TypeFacture.BON_COMMANDE) return "BON DE COMMANDE";
        if (f.getTypeFacture() == Facture.TypeFacture.PROFORMA) return "PROFORMA";
        if (f.getTypeFacture() == Facture.TypeFacture.AVOIR) return "AVOIR";
        return "FACTURE";
    }

    /** Ligne du tableau des désignations (6 colonnes). */
    private void addLigne(PdfPTable table, Font f, String designation, Integer qte,
                          BigDecimal pu, BigDecimal ht, BigDecimal tva, BigDecimal ttc) {
        PdfPCell c1 = new PdfPCell(new Phrase(nvl(designation), f));
        c1.setPadding(4);
        table.addCell(c1);
        table.addCell(cell(String.valueOf(qte != null ? qte : 1), f));
        table.addCell(cell(fmt(pu), f));
        table.addCell(cell(fmt(ht), f));
        table.addCell(cell(fmt(tva), f));
        PdfPCell c6 = new PdfPCell(new Phrase(fmt(ttc), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
        c6.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c6.setPadding(4);
        table.addCell(c6);
    }

    private void addInfoRow(PdfPTable t, String label, String valeur) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA, 9);
        t.addCell(new PdfPCell(new Phrase(label + " : ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9))));
        t.addCell(new PdfPCell(new Phrase(nvl(valeur), f)));
    }

    private void addRow(PdfPTable table, String label, BigDecimal value, boolean bold) {
        Font f = bold ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)
                : FontFactory.getFont(FontFactory.HELVETICA, 10);
        PdfPCell c1 = new PdfPCell(new Phrase(label, f));
        c1.setBorder(PdfPCell.NO_BORDER);
        c1.setPadding(2);
        table.addCell(c1);
        PdfPCell c2 = new PdfPCell(new Phrase(fmt(value), f));
        c2.setBorder(PdfPCell.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(2);
        table.addCell(c2);
    }

    private PdfPCell cell(String v, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(v, f));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setPadding(4);
        return c;
    }

    private String fmt(BigDecimal v) {
        return v != null ? String.format(Locale.FRENCH, "%,.2f", v) : "—";
    }

    private String nvl(String s) {
        return s != null && !s.isBlank() ? s : "—";
    }

    private BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}

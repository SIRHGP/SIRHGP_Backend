// ─── PaiePdfService.java ─────────────────────────────────────────────────────
package com.example.backend_sirhgp.service;

import com.example.backend_sirhgp.model.BulletinPaie;
import com.example.backend_sirhgp.model.Employer;
import com.example.backend_sirhgp.model.Entreprise;
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
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;

/**
 * Génération d'un bulletin de paie au format PDF (OpenPDF).
 * Mise en page simple et lisible.
 */
@Service
public class PaiePdfService {

    private static final String[] MOIS = {
            "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    };
    private static final String[] UNITES = {"", "un", "deux", "trois", "quatre", "cinq", "six", "sept",
            "huit", "neuf", "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize",
            "dix-sept", "dix-huit", "dix-neuf"};
    private static final String[] DIZAINES = {"", "", "vingt", "trente", "quarante", "cinquante",
            "soixante", "soixante", "quatre-vingt", "quatre-vingt"};

    public byte[] generate(BulletinPaie b) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Entreprise ent = b.getEntreprise();
            Employer emp = b.getEmploye();
            writeHeader(doc, b, ent, emp);

            // Éléments
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            int joursOuvres = PaiePdfService.joursOuvres(b.getMois(), b.getAnnee());
            BigDecimal tauxJournalier = (b.getSalaireBase() != null && joursOuvres > 0)
                    ? b.getSalaireBase().divide(BigDecimal.valueOf(joursOuvres), 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            addRow(table, "Salaire brut", b.getSalaireBase(), true);
            addRow(table, "Primes", b.getPrimes());
            addRow(table, "Indemnités", b.getIndemnites());

            addRowJours(table, "Jours ouvrables (mois)", joursOuvres);
            addRow(table, "Taux journalier", tauxJournalier);
            addRow(table, "CNPS salariale (4,4 %)", b.getCnpsSalariale());
            addRow(table, "IRPP (barème progressif)", b.getIrpp());
            addRow(table, "PVID (1,75 %)", b.getPvid());
            addRow(table, "CFC (0,11 %)", b.getCfc());
            addRow(table, "(absences / retards)", b.getRetenuesDiverses());
            addRow(table, "TOTAL RETENUES", b.getTotalRetenues(), true);

            addRow(table, "NET À PAYER", b.getNetAPayer(), true);
            addRow(table, "Coût total employeur", b.getCoutTotalEmployeur());
            doc.add(table);

            // ── Net en lettres ──
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Net à payer en lettres : " + enLettres(b.getNetAPayer()) + " francs CFA.",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));

            // ── Zone réservée CNPS ──
            doc.add(new Paragraph(" "));
            PdfPTable zone = new PdfPTable(1);
            zone.setWidthPercentage(100);
            PdfPCell zc = new PdfPCell(new Phrase("Zone réservée CNPS — période " + b.getMois() + "/"
                    + b.getAnnee() + " · assiette " + fmt(b.getBrut()),
                    FontFactory.getFont(FontFactory.HELVETICA, 8)));
            zc.setFixedHeight(40);
            zone.addCell(zc);
            doc.add(zone);

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Erreur génération PDF : " + e.getMessage(), e);
        }
    }

    /** Bloc employeur + identification employé (mentions légales, art. 92-93 Code du travail). */
    private void writeHeader(Document doc, BulletinPaie b, Entreprise ent, Employer emp) throws DocumentException {
        PdfPTable head = new PdfPTable(2);
        head.setWidthPercentage(100);
        PdfPCell cEmp = new PdfPCell();
        cEmp.setBorder(PdfPCell.NO_BORDER);
        cEmp.addElement(new Paragraph(ent != null ? ent.getName() : "—",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        if (ent != null) {
            if (ent.getRccm() != null) cEmp.addElement(new Paragraph("RCCM : " + ent.getRccm(),
                    FontFactory.getFont(FontFactory.HELVETICA, 9)));
            if (ent.getNumeroCnpsEmployeur() != null) cEmp.addElement(new Paragraph(
                    "N° employeur CNPS : " + ent.getNumeroCnpsEmployeur(),
                    FontFactory.getFont(FontFactory.HELVETICA, 9)));
            String addr = (ent.getAdresse() != null ? ent.getAdresse() + " " : "")
                    + (ent.getVille() != null ? ent.getVille() : "");
            if (!addr.isBlank()) cEmp.addElement(new Paragraph(addr.trim(),
                    FontFactory.getFont(FontFactory.HELVETICA, 9)));
        }
        PdfPCell cTitre = new PdfPCell();
        cTitre.setBorder(PdfPCell.NO_BORDER);
        cTitre.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cTitre.addElement(new Paragraph("BULLETIN DE PAIE",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
        cTitre.addElement(new Paragraph(MOIS[(b.getMois() - 1) % 12] + " " + b.getAnnee(),
                FontFactory.getFont(FontFactory.HELVETICA, 11)));
        head.addCell(cEmp);
        head.addCell(cTitre);
        doc.add(head);
        doc.add(new Paragraph(" "));

        doc.add(new Paragraph("Employé : " + (emp != null ? emp.getName() : "—")
                        + "   ·   NUI : " + (emp != null && emp.getNui() != null ? emp.getNui() : "—")
                        + "   ·   RIB : " + (emp != null && emp.getRib() != null ? emp.getRib() : "—"),
                FontFactory.getFont(FontFactory.HELVETICA, 10)));
        doc.add(new Paragraph("Poste : " + (emp != null && emp.getPoste() != null ? emp.getPoste().getName() : "—")
                        + "   ·   Date d'embauche : " + (emp != null && emp.getDateEmbauche() != null ? emp.getDateEmbauche().toString() : "—"),
                FontFactory.getFont(FontFactory.HELVETICA, 10)));
        doc.add(new Paragraph(" "));
    }

    private String fmt(BigDecimal v) {
        return v != null ? String.format(Locale.FRENCH, "%,.2f FCFA", v) : "—";
    }

    /** Nombre de jours ouvrables (lun–ven) d'un mois donné. */
    private static int joursOuvres(Integer mois, Integer annee) {
        if (mois == null || annee == null) return 0;
        java.time.YearMonth ym = java.time.YearMonth.of(annee, mois);
        int n = 0;
        for (java.time.LocalDate d = ym.atDay(1); d.getMonthValue() == mois; d = d.plusDays(1)) {
            java.time.DayOfWeek w = d.getDayOfWeek();
            if (w != java.time.DayOfWeek.SATURDAY && w != java.time.DayOfWeek.SUNDAY) n++;
        }
        return n;
    }

    /** Ligne avec une valeur affichée telle quelle (ex. nombre de jours), sans devise. */
    private void addRowJours(PdfPTable table, String label, int value) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA, 10);
        table.addCell(new PdfPCell(new Phrase(label, f)));
        PdfPCell cell = new PdfPCell(new Phrase(String.valueOf(value), f));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
    }

    /** Conversion du montant en lettres (français). */
    static String enLettres(BigDecimal montant) {
        if (montant == null) return "zéro";
        long n = montant.setScale(0, java.math.RoundingMode.HALF_UP).longValue();
        if (n == 0) return "zéro";
        StringBuilder sb = new StringBuilder();
        long millions = n / 1_000_000; n %= 1_000_000;
        long milliers = n / 1000; n %= 1000;
        if (millions > 0) sb.append(sousCentaine((int) millions)).append(millions > 1 ? " millions " : " million ");
        if (milliers > 0) sb.append(milliers > 1 ? sousCentaine((int) milliers) + " " : "").append("mille ");
        if (n > 0) sb.append(sousCentaine((int) n));
        String s = sb.toString().trim();
        if (s.isEmpty()) s = "zéro";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String sousCentaine(int n) {
        if (n < 20) return UNITES[n];
        if (n < 100) {
            int d = n / 10, u = n % 10;
            String base = DIZAINES[d];
            String fin = (u == 0) ? (d == 8 ? "s" : "")
                    : (d == 7 || d == 9 ? "-" + UNITES[10 + u]
                    : (u == 1 && d < 8 ? "-et-un" : "-" + UNITES[u]));
            return base + fin;
        }
        int c = n / 100, r = n % 100;
        String prefix = c > 1 ? UNITES[c] + " " : "";
        String suffix = (r == 0 && c > 1) ? "cents" : "cent" + (r > 0 ? " " + sousCentaine(r) : "");
        return prefix + suffix;
    }

    private void addRow(PdfPTable table, String label, BigDecimal value) {
        addRow(table, label, value, false);
    }

    private void addRow(PdfPTable table, String label, BigDecimal value, boolean bold) {
        Font f = bold ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)
                : FontFactory.getFont(FontFactory.HELVETICA, 10);
        table.addCell(new PdfPCell(new Phrase(label, f)));
        String v = value != null ? String.format(Locale.FRENCH, "%,.2f FCFA", value) : "—";
        PdfPCell cell = new PdfPCell(new Phrase(v, f));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
    }
}
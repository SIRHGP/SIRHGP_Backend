-- Harmonise les lignes de ventes marquees exonerees avec l'en-tete de facture.
-- L'HT, la TVA d'en-tete, le TTC et les paiements ne sont pas modifies.
UPDATE lignes_facture l
JOIN factures f ON f.id = l.facture_id
SET l.montanttva = 0.00
WHERE f.sens = 'VENTE'
  AND f.assujettitva = 0
  AND f.montanttva = 0.00
  AND l.montanttva <> 0.00;

-- Corrige uniquement les factures marquees PAYEE sans somme payee ni ecriture
-- dans le journal paiements_facture. Les montants sont conserves.
UPDATE factures f
SET f.statut = 'BROUILLON'
WHERE f.statut = 'PAYEE'
  AND f.montant_paye = 0
  AND f.montantttc > 0
  AND NOT EXISTS (
      SELECT 1 FROM paiements_facture p WHERE p.facture_id = f.id
  );

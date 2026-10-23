-- Ajoute les types pris en charge par Evenement.TypeEvenement.
-- Les valeurs existantes sont conservées et l'opération est additive.
ALTER TABLE evenements
    MODIFY COLUMN type ENUM(
        'REUNION', 'FORMATION', 'CONGE', 'MISSION', 'ENTREVUE', 'DEADLINE', 'AUTRE'
    ) NOT NULL;

-- Migration idempotente pour conserver l'ordre des lignes de facture.
-- Legacy rows without an order are initialized by ascending ID.
-- Run on sirhgp_db after a backup, with a user allowed to ALTER tables.

DELIMITER $$

DROP PROCEDURE IF EXISTS migration_20261021_ordre_lignes_facture$$

CREATE PROCEDURE migration_20261021_ordre_lignes_facture()
BEGIN
    DECLARE colonne_existe INT DEFAULT 0;
    DECLARE table_existe INT DEFAULT 0;
    DECLARE valeurs_manquantes INT DEFAULT 0;

    IF DATABASE() <> 'sirhgp_db' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Migration refusee: selectionner la base sirhgp_db.';
    END IF;

    SELECT COUNT(*) INTO table_existe
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lignes_facture';

    IF table_existe = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Migration refusee: table lignes_facture absente.';
    END IF;

    SELECT COUNT(*) INTO colonne_existe
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lignes_facture'
      AND COLUMN_NAME = 'ordre_ligne';

    IF colonne_existe = 0 THEN
        ALTER TABLE lignes_facture ADD COLUMN ordre_ligne INT NULL;
    END IF;

    UPDATE lignes_facture AS ligne
    JOIN (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY facture_id ORDER BY id) - 1 AS position_ligne
        FROM lignes_facture
    ) AS ordre ON ordre.id = ligne.id
    SET ligne.ordre_ligne = ordre.position_ligne
    WHERE ligne.ordre_ligne IS NULL;

    SELECT COUNT(*) INTO valeurs_manquantes
    FROM lignes_facture
    WHERE ordre_ligne IS NULL;

    IF valeurs_manquantes > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Migration refusee: des lignes n ont pas pu recevoir un ordre.';
    END IF;

    ALTER TABLE lignes_facture MODIFY COLUMN ordre_ligne INT NOT NULL;
END$$

CALL migration_20261021_ordre_lignes_facture()$$
DROP PROCEDURE migration_20261021_ordre_lignes_facture$$

DELIMITER ;

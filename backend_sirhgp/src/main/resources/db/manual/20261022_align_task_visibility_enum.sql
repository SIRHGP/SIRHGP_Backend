-- Aligne taches.visibilite sur Tache.Visibilite, sans perdre l'ancien statut.
-- Les anciennes taches publiques deviennent visibles a l'echelle entreprise.
DELIMITER $$
DROP PROCEDURE IF EXISTS align_task_visibility_enum_20261022$$
CREATE PROCEDURE align_task_visibility_enum_20261022()
BEGIN
    IF DATABASE() <> 'sirhgp_db' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Migration refusee: selectionner la base sirhgp_db.';
    END IF;

    ALTER TABLE taches
        MODIFY COLUMN visibilite ENUM('PRIVEE', 'SUCURSALE', 'ENTREPRISE', 'PUBLIQUE') NULL;

    UPDATE taches
    SET visibilite = CASE
        WHEN visibilite = 'PUBLIQUE' THEN 'ENTREPRISE'
        WHEN visibilite IS NULL AND sucursal_id IS NOT NULL THEN 'SUCURSALE'
        WHEN visibilite IS NULL AND entreprise_id IS NOT NULL THEN 'ENTREPRISE'
        WHEN visibilite IS NULL THEN 'PRIVEE'
        ELSE visibilite
    END
    WHERE visibilite = 'PUBLIQUE' OR visibilite IS NULL;

    ALTER TABLE taches
        MODIFY COLUMN visibilite ENUM('PRIVEE', 'SUCURSALE', 'ENTREPRISE') NOT NULL;
END$$
DELIMITER ;

CALL align_task_visibility_enum_20261022();
DROP PROCEDURE align_task_visibility_enum_20261022;

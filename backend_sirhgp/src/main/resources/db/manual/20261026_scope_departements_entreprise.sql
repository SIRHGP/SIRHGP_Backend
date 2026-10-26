-- Multi-tenant isolation for departments.
-- Run this once against the production database (defaultdb) before deploying
-- the application version that maps Departement.entreprise.
-- Take a database backup first. Departments with ambiguous ownership are left
-- unassigned and therefore visible only to SUPER_ADMIN until manually reviewed.

DELIMITER $$

DROP PROCEDURE IF EXISTS migration_20261026_scope_departements_entreprise$$

CREATE PROCEDURE migration_20261026_scope_departements_entreprise()
BEGIN
    DECLARE colonne_existe INT DEFAULT 0;
    DECLARE index_tenant_nom_existe INT DEFAULT 0;
    DECLARE contrainte_existe INT DEFAULT 0;
    DECLARE ancien_index VARCHAR(64);

    IF DATABASE() <> 'defaultdb' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Migration refusee: selectionner la base defaultdb.';
    END IF;

    SELECT COUNT(*) INTO colonne_existe
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'departements'
      AND COLUMN_NAME = 'entreprise_id';

    IF colonne_existe = 0 THEN
        ALTER TABLE departements ADD COLUMN entreprise_id BIGINT NULL;
    END IF;

    -- Older invoices may have been created before the tenant FK was filled.
    UPDATE factures f
    JOIN employers e ON e.id = f.emetteur_id
    SET f.entreprise_id = e.entreprise_id
    WHERE f.entreprise_id IS NULL AND e.entreprise_id IS NOT NULL;

    UPDATE factures f
    JOIN projets p ON p.id = f.projet_id
    SET f.entreprise_id = p.entreprise_id
    WHERE f.entreprise_id IS NULL AND p.entreprise_id IS NOT NULL;

    UPDATE facturation_cycles c
    JOIN projets p ON p.id = c.projet_id
    SET c.entreprise_id = p.entreprise_id
    WHERE c.entreprise_id IS NULL AND p.entreprise_id IS NOT NULL;

    -- Backfill only when every existing association points to one company.
    -- Supervisor, assigned employees, and project links are considered.
    UPDATE departements d
    JOIN (
        SELECT proprietaires.departement_id, MIN(proprietaires.entreprise_id) AS entreprise_id
        FROM (
            SELECT d0.id AS departement_id, e.entreprise_id
            FROM departements d0
            JOIN employers e ON e.id = d0.superviseur_id
            WHERE e.entreprise_id IS NOT NULL

            UNION ALL

            SELECT p.departement_id, e.entreprise_id
            FROM postes p
            JOIN employers e ON e.poste_id = p.id
            WHERE p.departement_id IS NOT NULL AND e.entreprise_id IS NOT NULL

            UNION ALL

            SELECT pd.departement_id, p.entreprise_id
            FROM projet_departements pd
            JOIN projets p ON p.id = pd.projet_id
            WHERE p.entreprise_id IS NOT NULL
        ) proprietaires
        GROUP BY proprietaires.departement_id
        HAVING COUNT(DISTINCT proprietaires.entreprise_id) = 1
    ) attribution ON attribution.departement_id = d.id
    SET d.entreprise_id = attribution.entreprise_id
    WHERE d.entreprise_id IS NULL;

    -- Replace the old global unique name constraint with per-company uniqueness.
    SELECT MIN(candidates.index_name) INTO ancien_index
    FROM (
        SELECT index_name
        FROM information_schema.statistics
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'departements'
          AND NON_UNIQUE = 0
        GROUP BY index_name
        HAVING COUNT(*) = 1 AND MAX(column_name) = 'name'
    ) candidates;

    IF ancien_index IS NOT NULL THEN
        SET @drop_index_sql = CONCAT('ALTER TABLE departements DROP INDEX `', ancien_index, '`');
        PREPARE drop_index_stmt FROM @drop_index_sql;
        EXECUTE drop_index_stmt;
        DEALLOCATE PREPARE drop_index_stmt;
    END IF;

    SELECT COUNT(*) INTO index_tenant_nom_existe
    FROM information_schema.statistics
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'departements'
      AND INDEX_NAME = 'uk_departement_entreprise_name';

    IF index_tenant_nom_existe = 0 THEN
        ALTER TABLE departements
            ADD CONSTRAINT uk_departement_entreprise_name UNIQUE (entreprise_id, name);
    END IF;

    SELECT COUNT(*) INTO contrainte_existe
    FROM information_schema.table_constraints
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'departements'
      AND CONSTRAINT_NAME = 'fk_departements_entreprise';

    IF contrainte_existe = 0 THEN
        ALTER TABLE departements
            ADD CONSTRAINT fk_departements_entreprise
            FOREIGN KEY (entreprise_id) REFERENCES entreprises(id);
    END IF;
END$$

CALL migration_20261026_scope_departements_entreprise()$$
DROP PROCEDURE migration_20261026_scope_departements_entreprise$$

DELIMITER ;

-- Review any unassigned or ambiguous records before giving tenant users access.
SELECT id, name
FROM departements
WHERE entreprise_id IS NULL
ORDER BY id;

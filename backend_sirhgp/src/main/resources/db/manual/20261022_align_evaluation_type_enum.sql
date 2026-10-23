-- Align the live MySQL enum with Evaluation.TypeEvaluation.
-- Safe only after confirming there are no values outside the current Java enum.
-- This script deliberately refuses to discard legacy values.
DELIMITER $$
DROP PROCEDURE IF EXISTS align_evaluation_type_enum_20261022$$

CREATE PROCEDURE align_evaluation_type_enum_20261022()
BEGIN
    DECLARE unsupported_count BIGINT DEFAULT 0;

    SELECT COUNT(*) INTO unsupported_count
    FROM evaluations
    WHERE type IS NOT NULL
      AND type NOT IN ('ANNUELLE', 'TRIMESTRIELLE', 'A_DATE');

    IF unsupported_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Migration interrompue: evaluations.type contient des valeurs hors enum Java';
    END IF;

    ALTER TABLE evaluations
        MODIFY COLUMN type ENUM('ANNUELLE', 'TRIMESTRIELLE', 'A_DATE') NOT NULL;
END$$
DELIMITER ;

CALL align_evaluation_type_enum_20261022();
DROP PROCEDURE align_evaluation_type_enum_20261022;

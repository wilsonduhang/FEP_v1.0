-- V19_1__dir_map_trigger_mysql.sql
-- P3a D6/D8 invariant: t_dir_map_config must hold exactly 88 rows.
-- MySQL 8 only; loaded via application-prod.yml's explicit
-- spring.flyway.locations entry (classpath:db/migration-mysql).
--
-- This file is intentionally located OUTSIDE db/migration. Flyway 10.x
-- recursively scans every classpath: location, so a child path such as
-- db/migration/mysql/ would be picked up by H2 dev/test (which lists
-- only classpath:db/migration) and fail because H2's parser does not
-- understand DELIMITER. The {vendor} placeholder is NOT a workaround:
-- Flyway identifies H2 as vendor "H2" regardless of MODE=MySQL, so
-- {vendor} cannot multiplex this file between dialects.
--
-- D8 row-count invariant on H2 is enforced at the Service layer via
-- Assert.state(count == 88) — see T5.
--
-- DELIMITER directives below are handled by Flyway's MySQLParser (token
-- type NEW_DELIMITER); they are NOT forwarded to MySQL. This was verified
-- against flyway-mysql 10.10.0 bytecode during T1 quality review. If a
-- future Flyway upgrade drops this support the migration will fail loudly
-- (CREATE TRIGGER body terminates at first ';'), so silent breakage is
-- not a risk.

DELIMITER //

CREATE TRIGGER IF NOT EXISTS trg_dir_map_config_no_insert_overflow
AFTER INSERT ON t_dir_map_config
FOR EACH ROW
BEGIN
    DECLARE current_count INT;
    SELECT COUNT(*) INTO current_count FROM t_dir_map_config;
    IF current_count > 88 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'P3a D6/D8 invariant: t_dir_map_config must hold exactly 88 rows; INSERT rejected (would exceed 88)';
    END IF;
END //

CREATE TRIGGER IF NOT EXISTS trg_dir_map_config_no_delete_underflow
AFTER DELETE ON t_dir_map_config
FOR EACH ROW
BEGIN
    DECLARE current_count INT;
    SELECT COUNT(*) INTO current_count FROM t_dir_map_config;
    IF current_count < 88 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'P3a D6/D8 invariant: t_dir_map_config must hold exactly 88 rows; DELETE rejected (would underflow)';
    END IF;
END //

DELIMITER ;

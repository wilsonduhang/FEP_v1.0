-- V19__create_dir_map_config.sql
-- P3a FR-MSG-DIR-MAP-CONFIG: dynamic direction map config table.
-- Supports H2 MODE=MySQL (test) and MySQL 8.0+ (prod).
-- F-level compliance: V1-V18 zero modification.

CREATE TABLE IF NOT EXISTS t_dir_map_config (
    message_type   VARCHAR(4)   NOT NULL,
    access_role    VARCHAR(32)  NOT NULL,
    direction      VARCHAR(32)  NOT NULL,
    requires_fep   BOOLEAN      NOT NULL,
    processing_mode VARCHAR(16) NOT NULL,
    updated_by     VARCHAR(64)  NOT NULL DEFAULT 'system',
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_dir_map_config PRIMARY KEY (message_type, access_role),
    CONSTRAINT chk_dir_map_role
        CHECK (access_role IN ('ACCEPTING_ORG', 'INFO_SERVICE_ORG')),
    CONSTRAINT chk_dir_map_direction
        CHECK (direction IN ('OUTBOUND_ACTIVE', 'INBOUND_PASSIVE', 'NOT_APPLICABLE')),
    CONSTRAINT chk_dir_map_mode
        CHECK (processing_mode IN ('MODE_1', 'MODE_2', 'MODE_3', 'MODE_5'))
);

CREATE INDEX IF NOT EXISTS idx_dir_map_role_fep
    ON t_dir_map_config (access_role, requires_fep);

CREATE TABLE IF NOT EXISTS t_dir_map_config_history (
    history_id      VARCHAR(32)  NOT NULL,
    message_type    VARCHAR(4)   NOT NULL,
    access_role     VARCHAR(32)  NOT NULL,
    old_direction   VARCHAR(32),
    old_requires_fep BOOLEAN,
    old_mode        VARCHAR(16),
    new_direction   VARCHAR(32)  NOT NULL,
    new_requires_fep BOOLEAN     NOT NULL,
    new_mode        VARCHAR(16)  NOT NULL,
    changed_by      VARCHAR(64)  NOT NULL,
    changed_at      TIMESTAMP    NOT NULL,
    change_reason   VARCHAR(500),
    CONSTRAINT pk_dir_map_history PRIMARY KEY (history_id)
);

CREATE INDEX IF NOT EXISTS idx_dir_map_history_target
    ON t_dir_map_config_history (message_type, access_role, changed_at DESC);

-- NOTE: MySQL row-count guard triggers live in
--   db/migration-mysql/V19_1__dir_map_trigger_mysql.sql
-- and are loaded only by application-prod.yml's explicit Flyway
-- location list (classpath:db/migration,classpath:db/migration-mysql).
--
-- Why a SIBLING directory (db/migration-mysql) rather than a child of
-- db/migration (db/migration/mysql)?  Flyway 10.x recursively scans
-- subfolders of every classpath: location it is given.  Putting the
-- trigger under db/migration/mysql/ would cause the H2 dev/test profile
-- (which only declares classpath:db/migration) to also load it; H2's
-- parser does not understand MySQL DELIMITER syntax and the dev boot
-- would fail with a SQL parse error.  A sibling location, gated behind
-- application-prod.yml, keeps H2 dev/test untouched.
--
-- (The {vendor} placeholder route was considered and rejected: Flyway
-- identifies H2 as vendor "H2" regardless of MODE=MySQL, so {vendor}
-- cannot be used to multiplex the trigger between H2 and MySQL.)
--
-- D8 row-count invariant on H2 is enforced at the Service layer via
-- Assert.state(count == 88) — see T5.

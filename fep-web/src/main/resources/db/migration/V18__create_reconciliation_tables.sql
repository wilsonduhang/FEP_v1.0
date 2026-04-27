-- P2e FR-PROC-RECON-* + FR-PROC-CLEAR-3115: reconciliation engine tables
-- Supports H2 MODE=MySQL (test) and MySQL 8.0+ (prod). For MySQL 5.7, use flyway-mysql extension.
--
-- Tables:
--   reconciliation_records       — PRD v1.3 §1983 (7 PRD fields + 6 P2e extensions = 13 total)
--   clearing_instruction_records — PRD v1.3 §1995 (8 PRD fields + 4 P2e extensions = 12 total)
--
-- ADR-P2e-3 transparent type deviations from PRD:
--   1. DECIMAL(20,4) instead of PRD DECIMAL(15,2) — supports HNDEMP cross-currency 4-digit precision
--   2. VARCHAR(64) account fields instead of PRD VARCHAR(32) — supports IBAN 34-char + bank prefix
--   3. paired_serial_no — non-PRD column for 3107↔3108 bidirectional traceability
--
-- F-level compliance: V1-V17 zero modification.

CREATE TABLE IF NOT EXISTS reconciliation_records (
    reconciliation_id        VARCHAR(32)   NOT NULL,
    reconciliation_date      DATE          NOT NULL,
    message_type             VARCHAR(4)    NOT NULL,
    serial_no                VARCHAR(64)   NOT NULL,
    paired_serial_no         VARCHAR(64),
    total_transaction_count  INT           NOT NULL DEFAULT 0,
    total_transaction_amount DECIMAL(20,4) NOT NULL DEFAULT 0.0000,
    actual_count             INT           NOT NULL DEFAULT 0,
    reconciliation_status    VARCHAR(20)   NOT NULL,
    discrepancy_count        INT           NOT NULL DEFAULT 0,
    reconciliation_time      TIMESTAMP     NOT NULL,
    created_at               TIMESTAMP     NOT NULL,
    updated_at               TIMESTAMP     NOT NULL,
    CONSTRAINT pk_recon PRIMARY KEY (reconciliation_id),
    CONSTRAINT chk_recon_message_type CHECK (message_type IN ('3107', '3108', '3116')),
    CONSTRAINT chk_recon_status CHECK (reconciliation_status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'DISCREPANCY')),
    CONSTRAINT uq_recon_serial_message UNIQUE (serial_no, message_type)
);

CREATE INDEX IF NOT EXISTS idx_recon_date_status ON reconciliation_records (reconciliation_date, reconciliation_status);
CREATE INDEX IF NOT EXISTS idx_recon_message_type_date ON reconciliation_records (message_type, reconciliation_date);
CREATE INDEX IF NOT EXISTS idx_recon_paired_serial ON reconciliation_records (paired_serial_no);

CREATE TABLE IF NOT EXISTS clearing_instruction_records (
    instruction_id     VARCHAR(32)   NOT NULL,
    qs_serial_no       VARCHAR(64)   NOT NULL,
    instruction_type   VARCHAR(20)   NOT NULL DEFAULT 'NORMAL',
    settlement_amount  DECIMAL(20,4) NOT NULL,
    payer_account      VARCHAR(64)   NOT NULL,
    payee_account      VARCHAR(64)   NOT NULL,
    instruction_status VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    execution_time     TIMESTAMP,
    failure_cause      VARCHAR(200),
    message_id         VARCHAR(32)   NOT NULL,
    created_at         TIMESTAMP     NOT NULL,
    updated_at         TIMESTAMP     NOT NULL,
    CONSTRAINT pk_clearing PRIMARY KEY (instruction_id, qs_serial_no),
    CONSTRAINT chk_clearing_type CHECK (instruction_type IN ('NORMAL', 'ERROR_HANDLING', 'BUSINESS_CANCEL')),
    CONSTRAINT chk_clearing_status CHECK (instruction_status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_clearing_status_date ON clearing_instruction_records (instruction_status, created_at);
CREATE INDEX IF NOT EXISTS idx_clearing_message_id ON clearing_instruction_records (message_id);

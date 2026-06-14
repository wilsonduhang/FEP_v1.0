-- §6.4.1 FR-DATA-DB-01: invoice verification tracking table (PRD v1.3 §1970).
-- Supports H2 MODE=MySQL (test) and MySQL 8.0+ (prod). For MySQL 5.7, use flyway-mysql extension.
--
-- Transparent deviations from PRD §1970:
--   1. invoice_amount DECIMAL(20,4) instead of PRD DECIMAL(12,2) — unified with reconciliation ADR-P2e-3.
--   2. verification_result stores the RAW HNDEMP return code (InvoCheckReturn3008.invoCheckReturnCode);
--      semantic ENUM mapping (核验通过/不通过/异常) is DEFERRED to the domain expert to avoid the
--      success-code dispute (see DEF-B2-2 ClearingInstructionService success-code inversion).
--   3. serial_no / created_at / updated_at — non-PRD columns for idempotency + audit (mirror reconciliation).
--
-- F-level compliance: V1-V37 zero modification.

CREATE TABLE IF NOT EXISTS invoice_verification_records (
    invoice_id          VARCHAR(32)   NOT NULL,
    invoice_code        VARCHAR(12),
    invoice_number      VARCHAR(8),
    invoice_amount      DECIMAL(20,4),
    invoice_date        DATE,
    verification_result VARCHAR(20)   NOT NULL,
    verification_time   TIMESTAMP     NOT NULL,
    failure_reason      VARCHAR(200),
    serial_no           VARCHAR(64)   NOT NULL,
    created_at          TIMESTAMP     NOT NULL,
    updated_at          TIMESTAMP     NOT NULL,
    CONSTRAINT pk_invoice_verif PRIMARY KEY (invoice_id),
    CONSTRAINT uq_invoice_verif_serial UNIQUE (serial_no)
);

CREATE INDEX IF NOT EXISTS idx_invoice_verif_time ON invoice_verification_records (verification_time);
CREATE INDEX IF NOT EXISTS idx_invoice_verif_code_num ON invoice_verification_records (invoice_code, invoice_number);

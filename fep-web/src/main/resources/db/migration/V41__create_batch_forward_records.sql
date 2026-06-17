-- §6.4.1 FR-DATA-DB-01: non-real-time (batch) business forward tracking table (PRD v1.3 §2020).
-- Supports H2 MODE=MySQL (test) and MySQL 8.0+ (prod). For MySQL 5.7, use flyway-mysql extension.
--
-- Transparent deviations from PRD §2020:
--   1. batch_type / batch_status store RAW derived values (msgNo / state-machine state name);
--      semantic ENUM mapping (合同备案/额度调整/数据同步/文件获取 ; 待处理/处理中/处理完成/处理异常)
--      is DEFERRED to the domain expert (mirror #96 DEF-B2-2 verification_result raw-code precedent).
--   2. error_log_path always NULL — FEP currently does not persist batch error-log files (zero fabrication).
--   3. serial_no / created_at / updated_at — non-PRD idempotency + audit columns (mirror V38).
--   4. PRD business defaults (batch_type=数据同步 / batch_status=待处理) NOT applied as SQL DEFAULT:
--      the event path always assigns these NOT NULL columns, so no default is ever reachable.
--
-- F-level compliance: V1-V40 zero modification.

CREATE TABLE IF NOT EXISTS batch_forward_records (
    batch_forward_id     VARCHAR(32)  NOT NULL,
    batch_type           VARCHAR(20)  NOT NULL,
    total_record_count   INT          NOT NULL,
    success_record_count INT          NOT NULL,
    process_start_time   TIMESTAMP    NOT NULL,
    process_end_time     TIMESTAMP,
    batch_status         VARCHAR(20)  NOT NULL,
    error_log_path       VARCHAR(200),
    serial_no            VARCHAR(64)  NOT NULL,
    created_at           TIMESTAMP    NOT NULL,
    updated_at           TIMESTAMP    NOT NULL,
    CONSTRAINT pk_batch_forward PRIMARY KEY (batch_forward_id),
    CONSTRAINT uq_batch_forward_serial UNIQUE (serial_no)
);

CREATE INDEX IF NOT EXISTS idx_batch_forward_start ON batch_forward_records (process_start_time);
CREATE INDEX IF NOT EXISTS idx_batch_forward_type ON batch_forward_records (batch_type);

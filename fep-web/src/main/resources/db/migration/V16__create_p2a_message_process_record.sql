-- P2a FR-PROC-STORE-JPA: message_process_record table + indices
-- Supports H2 MODE=MySQL and MySQL 8.0+. For MySQL 5.7, use flyway-mysql extension.

CREATE TABLE IF NOT EXISTS message_process_record (
    id              VARCHAR(32)   NOT NULL,
    message_type    VARCHAR(8)    NOT NULL,
    transition_no   VARCHAR(30)   NOT NULL,
    status          VARCHAR(16)   NOT NULL,
    created_at      BIGINT        NOT NULL,
    updated_at      BIGINT        NOT NULL,
    error_code      VARCHAR(16),
    error_message   VARCHAR(512),
    CONSTRAINT pk_mpr PRIMARY KEY (id),
    CONSTRAINT uk_mpr_transition_no UNIQUE (transition_no)
);

CREATE INDEX IF NOT EXISTS idx_mpr_status ON message_process_record (status);
CREATE INDEX IF NOT EXISTS idx_mpr_created_at ON message_process_record (created_at);

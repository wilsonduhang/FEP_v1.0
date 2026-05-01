-- V22__create_outbound_message_queue.sql
-- P4 FR-MSG-MODE-DW-ASSEMBLE + PRD §3.2: outbound message queue persisted by
-- fep-web JpaOutboundMessageEnqueueAdapter on behalf of fep-collector.
-- Supports H2 MODE=MySQL (test) and MySQL 8.0+ (prod).
--
-- Plan §T7a originally specified V20; bumped to V22 because V19/V20/V21 are
-- already taken by P3a dir-map work (V19 dir-map config DDL, V20 dir-map seed,
-- V21 dir-map menu). F-level compliance: V1-V21 zero modification.
--
-- Columns:
--   queue_id            32-char UUID (no dashes), primary key (IdGenerator.uuid32)
--   message_type        4-digit HNDEMP message type (e.g., "3101")
--   transition_no       8-digit business serial (PRD §3.2.3)
--   idempotency_key     32-char hex (P4 IdempotencyKeyGenerator), UNIQUE
--   message_head_xml    JAXB-marshalled OutboundHead (3 fields)
--   message_body_xml    JAXB-marshalled CFX body POJO (per messageType)
--   payload_data_type   business payload type tag (e.g., "INVOICE_CONTRACT_3101")
--   source_ref          source-system reference (row PK / file offset / ESB id)
--   status              "PENDING" | "PROCESSING" | "SENT" | "FAILED" (consumer set)
--   retry_count         starts 0; consumer increments on transient failure
--   next_retry_at       backoff target; null when not waiting
--   error_message       last error detail (consumer-populated)
--   created_at          row insert timestamp
--   updated_at          last mutation timestamp

CREATE TABLE IF NOT EXISTS outbound_message_queue (
    queue_id            VARCHAR(32) NOT NULL,
    message_type        VARCHAR(8)  NOT NULL,
    transition_no       VARCHAR(30) NOT NULL,
    idempotency_key     VARCHAR(64) NOT NULL,
    message_head_xml    TEXT        NOT NULL,
    message_body_xml    TEXT        NOT NULL,
    payload_data_type   VARCHAR(64) NOT NULL,
    source_ref          VARCHAR(255),
    status              VARCHAR(16) NOT NULL,
    retry_count         INT         NOT NULL DEFAULT 0,
    next_retry_at       TIMESTAMP,
    error_message       TEXT,
    created_at          TIMESTAMP   NOT NULL,
    updated_at          TIMESTAMP   NOT NULL,
    CONSTRAINT pk_outbound_queue PRIMARY KEY (queue_id),
    CONSTRAINT uk_outbound_queue_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_outbound_queue_status_created
    ON outbound_message_queue (status, created_at);

CREATE INDEX IF NOT EXISTS idx_outbound_queue_retry
    ON outbound_message_queue (status, next_retry_at);

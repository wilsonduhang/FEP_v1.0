-- V23: P4 T8 — collection_run + collection_record_offset (FR-MSG-MODE-DW-PERSIST, FR-MSG-MODE-DW-IDEMPOTENT)
--
-- PRD v1.3 §2.2.2 数仓模式持久化：
--   collection_run            — single row per CollectorScheduler.runAdapter invocation,
--                               written by JdbcCollectionRunRecorder (start RUNNING / complete terminal).
--   collection_record_offset  — per-adapter watermark cursor advanced after successful acknowledge,
--                               read/written by JdbcWatermarkStore.
--
-- Plan §T8 originally specified V19; V19/V20/V21 already taken by P3a dir-map work and
-- T7a took V22 (outbound_message_queue). T8 uses V23 as a single migration with both
-- tables to keep the P4 collector schema atomically introduced.
--
-- F-level compliance: V1-V22 zero modification.
-- Supports H2 MODE=MySQL (test) and MySQL 8.0+ (prod). Column types align with V22
-- (TIMESTAMP, no TIMESTAMP WITH TIME ZONE) for cross-vendor portability.
--
-- collection_run columns:
--   run_id           32-char UUID (no dashes), primary key (IdGenerator.uuid32)
--   adapter_id       configured adapter ID (CollectorProperties.Adapter#id), max 64
--   status           "RUNNING" | "SUCCESS" | "PARTIAL" | "FAILED" | "SKIPPED"
--                    — typed as VARCHAR(16) (CollectionRunResult.Status enum names fit)
--   started_at       Instant the scheduler took the lock + persisted RUNNING row
--   completed_at     Instant the recorder.complete fired (NULL while RUNNING)
--   collected_count  records returned from adapter.collect (T8 reserves this column for
--                    the future P5+ consumer to backfill; T6a scheduler does not yet
--                    distinguish collected vs assembled — see Plan §T8 #3)
--   assembled_count  PayloadAssembler.assemble OK count (incl. dup-key tolerance)
--   submitted_count  enqueuePort.submit OK count (incl. dup-key tolerance)
--   error_count      records that raised non-dup-key exceptions + orchestration failures
--   error_message    first error.getMessage() (capped at 1024 chars by scheduler)
--   trigger_source   "MANUAL" | "SCHEDULED" (CollectorScheduler TriggerType.name())
--   created_at       row insert timestamp (defensively populated by recorder.start)
--
-- collection_record_offset columns:
--   adapter_id  PRIMARY KEY — one watermark per adapter
--   watermark   opaque cursor string (encoding adapter-defined, e.g., ISO-8601 / PK / MQ offset)
--   updated_at  last write timestamp (advanced by JdbcWatermarkStore.put)

CREATE TABLE IF NOT EXISTS collection_run (
    run_id           VARCHAR(32)  NOT NULL,
    adapter_id       VARCHAR(64)  NOT NULL,
    status           VARCHAR(16)  NOT NULL,
    started_at       TIMESTAMP    NOT NULL,
    completed_at     TIMESTAMP,
    collected_count  INT          NOT NULL DEFAULT 0,
    assembled_count  INT          NOT NULL DEFAULT 0,
    submitted_count  INT          NOT NULL DEFAULT 0,
    error_count      INT          NOT NULL DEFAULT 0,
    error_message    TEXT,
    trigger_source   VARCHAR(32)  NOT NULL,
    created_at       TIMESTAMP    NOT NULL,
    CONSTRAINT pk_collection_run PRIMARY KEY (run_id)
);

CREATE INDEX IF NOT EXISTS idx_collection_run_adapter_started
    ON collection_run (adapter_id, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_collection_run_status
    ON collection_run (status);

CREATE TABLE IF NOT EXISTS collection_record_offset (
    adapter_id  VARCHAR(64)  NOT NULL,
    watermark   VARCHAR(128) NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    CONSTRAINT pk_collection_record_offset PRIMARY KEY (adapter_id)
);

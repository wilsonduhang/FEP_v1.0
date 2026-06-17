-- §5.8 FR-WEB-AUDIT-REVIEW: 业务规则失败报文的人工审核任务表（Phase2 单级）。
-- PRD v1.3 §5.8 数据校验与审核 功能点 2「多级审核：系统自动校验 → 业务人员人工审核（可配置）」。
--
-- 数据来源：JpaMessageProcessStore.updateStatus 旁路 —— 当报文落 FAILED 且 error_code=PROC_8507
-- （业务规则违规）时额外创建一条审核任务；原 message_process_record 仍照常落 FAILED（状态机不变）。
--
-- 覆盖范围（Plan D5）：Sync/Async 单条报文（经 failWith 落 PROC_8507）。
-- Batch 逐条审核 deferred（batch 经 transition(...FAILED) 不落 error_code，逐条违规仅存 BatchResult 内存）。
--
-- Supports H2 MODE=MySQL (test) and MySQL 8.0+ (prod).
-- F-level compliance: V1-V40 zero modification.

CREATE TABLE IF NOT EXISTS message_review_task (
    review_id            VARCHAR(32)   NOT NULL,
    message_record_id    VARCHAR(32)   NOT NULL,
    message_type         VARCHAR(8)    NOT NULL,
    transition_no        VARCHAR(30)   NOT NULL,
    error_code           VARCHAR(16)   NOT NULL,
    violation_summary    VARCHAR(512),
    review_status        VARCHAR(16)   NOT NULL,
    review_level         INT           NOT NULL DEFAULT 1,
    current_level        INT           NOT NULL DEFAULT 1,
    assigned_reviewer_id VARCHAR(32),
    reviewer_id          VARCHAR(32),
    review_comment       VARCHAR(500),
    created_at           BIGINT        NOT NULL,
    reviewed_at          BIGINT,
    CONSTRAINT pk_msg_review PRIMARY KEY (review_id),
    CONSTRAINT uq_msg_review_record UNIQUE (message_record_id)
);

CREATE INDEX IF NOT EXISTS idx_msg_review_status ON message_review_task (review_status);
CREATE INDEX IF NOT EXISTS idx_msg_review_created ON message_review_task (created_at);

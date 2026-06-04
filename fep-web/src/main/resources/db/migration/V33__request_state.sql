-- S2 request-state tracking T1: 请求生命周期聚合表（CREATED→SENT→RESULT_RECEIVED + FAILED/STUCK 5 状态）
-- retrofit 既有 outbound 流（OutboundMessageQueueEntity + OutboundStatusWriterService）+ inbound 流
-- （InboundMessageProcessedEvent），correlation key = 8 位业务 transitionNo（两侧唯一共有）。
-- 镜像 V32 in_app_notification DDL 风格（H2 MODE=MySQL / MySQL / PostgreSQL 兼容子集：BOOLEAN/TIMESTAMP）。
--   correlation_blocked — 结构性永等不到匹配的请求隔离列（P3 Phase2 platPayNo 占位 3115 链，见
--     docs/decisions/2026-05-05-inbound-realhead-extraction-blocked.md §3.2）；reaper findStuck 排除该列
--     为 true 的行，避免 STUCK 计数被已知结构性缺口污染（红线 audit_maturity_label_needs_prd_trace）。
CREATE TABLE t_request_state (
  request_state_id      VARCHAR(32)  NOT NULL,
  correlation_key       VARCHAR(32)  NOT NULL,
  message_type          VARCHAR(8)   NOT NULL,
  outbound_queue_id     VARCHAR(32)  DEFAULT NULL,
  lifecycle_status      VARCHAR(16)  NOT NULL,
  correlation_blocked   BOOLEAN      NOT NULL DEFAULT FALSE,
  inbound_serial_no     VARCHAR(64)  DEFAULT NULL,
  inbound_transition_no VARCHAR(8)   DEFAULT NULL,
  created_at            TIMESTAMP    NOT NULL,
  sent_at               TIMESTAMP    DEFAULT NULL,
  result_received_at    TIMESTAMP    DEFAULT NULL,
  updated_at            TIMESTAMP    NOT NULL,
  PRIMARY KEY (request_state_id),
  CONSTRAINT uk_request_state_correlation UNIQUE (correlation_key)
);
CREATE INDEX idx_request_state_lifecycle ON t_request_state (lifecycle_status);
CREATE INDEX idx_request_state_stuck ON t_request_state (lifecycle_status, correlation_blocked, updated_at);

-- V25__outbound_queue_observability_columns.sql
-- ADR docs/decisions/2026-05-04-outbound-status-machine.md §5 仲裁方案 B 落地。
--
-- V22 已 ship outbound_message_queue 表（含 status/retry_count/next_retry_at/error_message）。
-- V23/V24 已 ship（P4 collection_run + P4 seed_collector_menu）— 原 Plan v0.6 V24 改为 V25
-- 因 V24 已被 P4 T10 commit cff6972 占用。
--
-- V25 增量加 3 列（不动 V22 — Flyway F 级硬冻结）+ 6 值 status 声明（不再加索引）：
--   sent_at         TLQ 发送成功时间, NULL 表示未发送
--   msg_id          TLQ MsgId (PRD §3.1.3 14 datetime + 6 seq, 例 20260504103000000001)
--   tlq_send_result TLQ SendResult.toString() 截断 64 字符（成功 "ok:..." / 失败 "fail:..."）
--
-- v0.4 修订 F4 + v0.7 V25 改名: V25 不再创建 idx_outbound_queue_status_next_retry，
-- 复用 V22 行 48 既有 idx_outbound_queue_retry (status, next_retry_at) — 同列组等价。
--
-- status 列 6 值定义（V22 SQL line 19 注释仅列 4 值是 comment 不完整，以本文件 + JPA Javadoc 为准）：
--   PENDING — 入队初始态（P4 collector 写入）
--   PROCESSING — P5 consumer 持锁中
--   SENT — TLQ 发送成功（terminal-success）
--   FAILED — 单次失败瞬时态（短暂，立即转 RETRY 或 DEAD_LETTER）
--   RETRY — exp_backoff 退避中，next_retry_at 调度
--   DEAD_LETTER — retry_count >= 5 终止重试（terminal-DLQ）

ALTER TABLE outbound_message_queue ADD COLUMN sent_at         TIMESTAMP   NULL;
ALTER TABLE outbound_message_queue ADD COLUMN msg_id          VARCHAR(20) NULL;
ALTER TABLE outbound_message_queue ADD COLUMN tlq_send_result VARCHAR(64) NULL;

COMMENT ON COLUMN outbound_message_queue.status IS
  'PENDING|PROCESSING|SENT|FAILED|RETRY|DEAD_LETTER (6 values, V22 注释 line 19 仅列 4 值是 comment 不完整, 以 V25 + JPA Javadoc 为准)';
COMMENT ON COLUMN outbound_message_queue.sent_at IS 'TLQ 发送成功时间, NULL 表示未发送';
COMMENT ON COLUMN outbound_message_queue.msg_id IS 'TLQ MsgId (14 datetime + 6 seq, 例 20260504103000000001)';
COMMENT ON COLUMN outbound_message_queue.tlq_send_result IS 'TLQ SendResult.toString() 截断 64 字符';

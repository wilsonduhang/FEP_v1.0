-- Callback Phase 2b T8: DLQ 复制重放审计链（金融审计）
-- 镜像 V25/V29 explicit NULL convention（Flyway F 级硬冻结，新增列不动 V27/V29）。
--   original_dlq_id — 重放来源 DEAD_LETTER 行的 queue_id（审计回溯），NULL 表示非重放行
--   replayed_by     — 触发重放的 admin 用户 id，NULL 表示非重放行
--   replayed_at     — 重放时间，NULL 表示非重放行
ALTER TABLE callback_queue ADD COLUMN original_dlq_id VARCHAR(64) NULL;
ALTER TABLE callback_queue ADD COLUMN replayed_by     VARCHAR(64) NULL;
ALTER TABLE callback_queue ADD COLUMN replayed_at     TIMESTAMP   NULL;
CREATE INDEX idx_callback_queue_original_dlq ON callback_queue (original_dlq_id);

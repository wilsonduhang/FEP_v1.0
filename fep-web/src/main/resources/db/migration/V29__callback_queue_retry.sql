-- 接口模式回调 Phase 2：retry/DLQ + lease 字段（镜像 outbound_message_queue 模式）
ALTER TABLE callback_queue ADD COLUMN retry_count INT NOT NULL DEFAULT 0;
ALTER TABLE callback_queue ADD COLUMN next_retry_at TIMESTAMP NULL;
ALTER TABLE callback_queue ADD COLUMN claimed_at TIMESTAMP NULL;
-- claimBatch 过滤/排序索引：WHERE status=... ORDER BY next_retry_at
CREATE INDEX idx_callback_queue_status_nra ON callback_queue (status, next_retry_at);

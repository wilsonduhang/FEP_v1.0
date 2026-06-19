-- V43: message_review_task 复合索引——回填已存在的审核队列查询
-- `WHERE review_status = ? ORDER BY created_at DESC`（MessageReviewTaskService.list），
-- 消单列索引下的 filesort。不删既有 idx_msg_review_status / idx_msg_review_created
-- （其他 status-only / created-only 查询仍可用），纯增量加固，零行为变更。
CREATE INDEX IF NOT EXISTS idx_msg_review_status_created
    ON message_review_task (review_status, created_at DESC);

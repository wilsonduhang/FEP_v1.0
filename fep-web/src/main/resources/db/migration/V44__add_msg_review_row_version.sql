-- §5.8 FR-WEB-AUDIT-REVIEW: message_review_task 乐观锁版本列（并发审核决策冲突检测）。
-- 配合实体 @Version：审核决策（approve/reject）路径在并发下，持 stale 版本的写入命中 0 行 →
-- Hibernate 抛 OptimisticLockingFailureException，杜绝两审核人同时决策同一 PENDING 任务的丢失更新
-- 及多级 current_level 自增竞争。现有行（若有）DEFAULT 0；新建行 Hibernate persist 时置 0。
-- Supports H2 MODE=MySQL (test) and MySQL 8.0+ (prod).
-- F-level compliance: V1-V43 zero modification.
ALTER TABLE message_review_task ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;

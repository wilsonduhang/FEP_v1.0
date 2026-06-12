-- V36__operation_log_integrity.sql
-- GM S5: 审计日志完整性列（SM3 hash 链 + SM2 行签名 + TraceId，架构 §1219）
-- 全列 nullable：V36 前存量行为链外（不回填——历史完整性无法事后背书，Plan 抉择⑧）
ALTER TABLE t_sys_operation_log ADD COLUMN seq BIGINT DEFAULT NULL COMMENT '链序号（部署后首行=1，连续递增）';
ALTER TABLE t_sys_operation_log ADD COLUMN prev_hash VARCHAR(64) DEFAULT NULL COMMENT '前行 SM3 hash（链首=64个0）';
ALTER TABLE t_sys_operation_log ADD COLUMN hash VARCHAR(64) DEFAULT NULL COMMENT '本行 SM3 hash = SM3(prev_hash || canonical)';
ALTER TABLE t_sys_operation_log ADD COLUMN signature VARCHAR(120) DEFAULT NULL COMMENT 'SM2 裸签 Base64（r||s 64字节→88字符；mock 域为占位串）';
ALTER TABLE t_sys_operation_log ADD COLUMN sign_key_id VARCHAR(64) DEFAULT NULL COMMENT '签名时审计密钥版本';
ALTER TABLE t_sys_operation_log ADD COLUMN trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪 ID（TraceIdFilter MDC）';
ALTER TABLE t_sys_operation_log ADD CONSTRAINT uk_audit_seq UNIQUE (seq);
CREATE INDEX idx_log_trace ON t_sys_operation_log (trace_id);

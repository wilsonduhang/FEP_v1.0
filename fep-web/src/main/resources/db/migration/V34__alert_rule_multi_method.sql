-- Phase 2c-A: t_sys_alert_rule 单值 notify_method 升级为多值 notify_methods + 新增 alert_phone（SMS 收件号）。
-- 默认行翻为「启用 + IN_APP」以保留 Phase 2b IN_APP 常开语义（迁移前 IN_APP 由 listener 无条件发，
-- 统一引擎门控后须默认启用 IN_APP 否则行为回归）。参见 docs/plans/2026-06-05-callback-phase2c-a-alert-channels.md。

ALTER TABLE t_sys_alert_rule ADD COLUMN notify_methods VARCHAR(60) NOT NULL DEFAULT 'IN_APP' COMMENT '启用渠道集合，逗号连接 EMAIL/IN_APP/SMS';
ALTER TABLE t_sys_alert_rule ADD COLUMN alert_phone VARCHAR(50) DEFAULT NULL COMMENT 'SMS 告警收件手机号';

-- 回填：既有单值 notify_method 平移到 notify_methods
UPDATE t_sys_alert_rule SET notify_methods = notify_method;

-- 翻默认行：保留 IN_APP 常开（迁移前 IN_APP 无条件发）
UPDATE t_sys_alert_rule SET alert_enabled = TRUE, notify_methods = 'IN_APP'
 WHERE rule_id = 'default_alert_rule_00000000001';

ALTER TABLE t_sys_alert_rule DROP COLUMN notify_method;

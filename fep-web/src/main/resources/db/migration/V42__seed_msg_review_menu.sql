-- §5.8 FR-WEB-AUDIT-REVIEW: 在「数据校验审核」(DATA_AUDIT, V3 line 15, menu_id ...008) 下追加
-- 「待审核报文」子菜单（menu_id ...029，紧接现有最大 2000-段 ...028）。
-- PRD v1.3 §5.8 多级审核 Phase2。
--
-- 后端 API 已就绪（B8 Phase2 Task5）：
--   GET  /api/v1/audit/reviews             — 审核任务列表（按状态筛选 + 分页）
--   GET  /api/v1/audit/reviews/{id}        — 审核任务详情
--   PUT  /api/v1/audit/reviews/{id}/approve — 审核通过
--   PUT  /api/v1/audit/reviews/{id}/reject  — 审核驳回
--
-- Vue 前端路由由 Phase3 实现（依赖原型）；route_path 现先固化以待对齐。
-- 超管角色（role_id ...010）授予 view 权限（与既有报送/采集菜单一致）。
-- 细粒度 audit:review:approve / :reject 权限点 + 业务人员角色 → RBAC 对齐 ticket deferred
-- （镜像 V24 trigger 权限的 deferral）。
--
-- F-level compliance: V1-V41 zero modification.

INSERT INTO t_sys_menu (menu_id, menu_code, menu_name, parent_id, menu_level, menu_icon, sort_order, menu_status, route_path) VALUES
('20000000000000000000000000000029', 'MSG_REVIEW', '待审核报文', '10000000000000000000000000000008', 2, 'audit', 1, 'ACTIVE', '/audit/reviews');

INSERT INTO t_sys_role_permission (role_id, menu_id, permission_code) VALUES
('00000000000000000000000000000010', '20000000000000000000000000000029', 'view');

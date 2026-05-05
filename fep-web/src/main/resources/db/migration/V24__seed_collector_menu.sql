-- V24__seed_collector_menu.sql
-- P4 数据采集层 — 在"报送管理"父菜单 (ID=10000000000000000000000000000006，V3 line 13) 下追加
-- 2 个采集相关子菜单（sort_order 5/6，紧接 V11 已有的 4 项 1-4）。
-- PRD §5.5 报送管理 + §2.2.3 数据采集。
--
-- 后端 API 已就绪（P4 T6b commit e33be87）：
--   POST /api/v1/collector/triggers — 手动触发采集
--   GET  /api/v1/collector/runs     — 采集运行历史分页查询
--
-- 前端 Vue 路由由 P7 实现；本菜单 route_path 现先固化以待 P7 路由对齐
-- （沿用 V11 既有的 /report/<sub-path> 命名约定）。
--
-- 超管角色授予 view 权限（与 V11/V13/V15 已有 9 项报送菜单一致）。
-- 触发权限将在 P6d RBAC 对齐 ticket 中扩展为 'report:collector:trigger' 独立权限点
-- （Plan §T6 #6 / CollectorTriggerController.java:33 跟踪）。

INSERT INTO t_sys_menu (menu_id, menu_code, menu_name, parent_id, menu_level, menu_icon, sort_order, menu_status, route_path) VALUES
('20000000000000000000000000000027', 'COLLECTOR_RUNS',    '采集运行历史', '10000000000000000000000000000006', 2, 'history', 5, 'ACTIVE', '/report/collector/runs'),
('20000000000000000000000000000028', 'COLLECTOR_TRIGGER', '手动采集触发', '10000000000000000000000000000006', 2, 'play',    6, 'ACTIVE', '/report/collector/triggers');

-- 超管角色授予 view 权限（role_id = 00000000000000000000000000000010, V2 seed line 4）
INSERT INTO t_sys_role_permission (role_id, menu_id, permission_code) VALUES
('00000000000000000000000000000010', '20000000000000000000000000000027', 'view'),
('00000000000000000000000000000010', '20000000000000000000000000000028', 'view');

-- V9: P6c 企业信息查询管理菜单种子数据
-- 在已有"企业信息查询"父菜单（ID=10000000000000000000000000000004）下插入 2 个子菜单
-- 并为超管角色（ID=00000000000000000000000000000010）授予 view 权限
-- PRD §5.4 (FR-WEB-ENT)

-- 企业信息查询子菜单（层级 2，parent = 10000000000000000000000000000004）
INSERT INTO t_sys_menu (menu_id, menu_code, menu_name, parent_id, menu_level, menu_icon, sort_order, menu_status, route_path) VALUES
('20000000000000000000000000000008', 'ENT_QUERY_TASK', '查询任务管理', '10000000000000000000000000000004', 2, 'search',   1, 'ACTIVE', '/enterprise/query-tasks'),
('20000000000000000000000000000009', 'ENT_AUTH_LETTER', '授权书管理',   '10000000000000000000000000000004', 2, 'document', 2, 'ACTIVE', '/enterprise/auth-letters');

-- 超管角色授予 2 个子菜单 view 权限
INSERT INTO t_sys_role_permission (role_id, menu_id, permission_code)
SELECT '00000000000000000000000000000010', menu_id, 'view'
FROM t_sys_menu
WHERE menu_id IN (
    '20000000000000000000000000000008',
    '20000000000000000000000000000009'
);

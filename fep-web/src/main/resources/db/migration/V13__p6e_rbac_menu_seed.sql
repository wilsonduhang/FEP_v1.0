-- V13: P6e 首页看板 + 业务数据管理菜单种子数据
-- 在已有"首页"父菜单（ID=10000000000000000000000000000002）下插入 3 个子菜单
-- 在已有"业务数据管理"父菜单（ID=10000000000000000000000000000003）下插入 2 个子菜单
-- 并为超管角色（ID=00000000000000000000000000000010）授予 view 权限
-- PRD §5.2 + §5.3

-- 首页看板子菜单（层级 2，parent = 10000000000000000000000000000002）
INSERT INTO t_sys_menu (menu_id, menu_code, menu_name, parent_id, menu_level, menu_icon, sort_order, menu_status, route_path) VALUES
('20000000000000000000000000000019', 'DASH_TODO',     '待办事项', '10000000000000000000000000000002', 2, 'calendar', 1, 'ACTIVE', '/dashboard/todos'),
('20000000000000000000000000000020', 'DASH_STATS',    '数据统计', '10000000000000000000000000000002', 2, 'chart',    2, 'ACTIVE', '/dashboard/stats'),
('20000000000000000000000000000021', 'DASH_SHORTCUT', '快捷入口', '10000000000000000000000000000002', 2, 'link',     3, 'ACTIVE', '/dashboard/shortcuts');

-- 业务数据管理子菜单（层级 2，parent = 10000000000000000000000000000003）
INSERT INTO t_sys_menu (menu_id, menu_code, menu_name, parent_id, menu_level, menu_icon, sort_order, menu_status, route_path) VALUES
('20000000000000000000000000000022', 'BIZ_MSG_DEF',    '报文定义', '10000000000000000000000000000003', 2, 'document', 1, 'ACTIVE', '/biz/definitions'),
('20000000000000000000000000000023', 'BIZ_MSG_RECORD', '报文记录', '10000000000000000000000000000003', 2, 'list',     2, 'ACTIVE', '/biz/records');

-- 超管角色授予 5 个子菜单 view 权限
INSERT INTO t_sys_role_permission (role_id, menu_id, permission_code)
SELECT '00000000000000000000000000000010', menu_id, 'view'
FROM t_sys_menu
WHERE menu_id IN (
    '20000000000000000000000000000019',
    '20000000000000000000000000000020',
    '20000000000000000000000000000021',
    '20000000000000000000000000000022',
    '20000000000000000000000000000023'
);

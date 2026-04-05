-- 默认菜单树种子数据
-- 层级 1: 顶级模块
-- 层级 2: 子模块
-- 层级 3: 操作按钮（P6a.1 暂不细化）

-- 顶级: 系统管理 (menu_id=10000000000000000000000000000001)
INSERT INTO t_sys_menu (menu_id, menu_code, menu_name, parent_id, menu_level, menu_icon, sort_order, menu_status, route_path) VALUES
('10000000000000000000000000000001', 'SYS_MGMT',    '系统管理',     NULL, 1, 'setting',  900, 'ACTIVE', '/system'),
('10000000000000000000000000000002', 'DASHBOARD',   '首页',         NULL, 1, 'home',     100, 'ACTIVE', '/dashboard'),
('10000000000000000000000000000003', 'BIZ_DATA',    '业务数据管理', NULL, 1, 'database', 200, 'ACTIVE', '/biz'),
('10000000000000000000000000000004', 'ENT_QUERY',   '企业信息查询', NULL, 1, 'search',   300, 'ACTIVE', '/enterprise'),
('10000000000000000000000000000005', 'DATA_SUBMIT', '数据报送管理', NULL, 1, 'upload',   400, 'ACTIVE', '/submit'),
('10000000000000000000000000000006', 'REPORT_MGMT', '报送管理',     NULL, 1, 'file',     500, 'ACTIVE', '/report'),
('10000000000000000000000000000007', 'TLQ_NODE',    'TLQ 节点管理', NULL, 1, 'cluster',  600, 'ACTIVE', '/tlq'),
('10000000000000000000000000000008', 'DATA_AUDIT',  '数据校验审核', NULL, 1, 'check',    700, 'ACTIVE', '/audit'),
('10000000000000000000000000000009', 'OPS_MONITOR', '运维监控',     NULL, 1, 'monitor',  800, 'ACTIVE', '/ops');

-- 系统管理子模块（层级 2）
INSERT INTO t_sys_menu (menu_id, menu_code, menu_name, parent_id, menu_level, menu_icon, sort_order, menu_status, route_path) VALUES
('20000000000000000000000000000001', 'SYS_USER',   '用户管理',   '10000000000000000000000000000001', 2, 'user',     1, 'ACTIVE', '/system/user'),
('20000000000000000000000000000002', 'SYS_ROLE',   '角色管理',   '10000000000000000000000000000001', 2, 'team',     2, 'ACTIVE', '/system/role'),
('20000000000000000000000000000003', 'SYS_MENU',   '菜单管理',   '10000000000000000000000000000001', 2, 'menu',     3, 'ACTIVE', '/system/menu'),
('20000000000000000000000000000004', 'SYS_MSG',    '消息管理',   '10000000000000000000000000000001', 2, 'message',  4, 'ACTIVE', '/system/message'),
('20000000000000000000000000000005', 'SYS_DL',     '下载任务',   '10000000000000000000000000000001', 2, 'download', 5, 'ACTIVE', '/system/download'),
('20000000000000000000000000000006', 'SYS_LOG',    '日志管理',   '10000000000000000000000000000001', 2, 'history',  6, 'ACTIVE', '/system/log'),
('20000000000000000000000000000007', 'SYS_CONFIG', '系统配置',   '10000000000000000000000000000001', 2, 'tool',     7, 'ACTIVE', '/system/config');

-- 超管角色授予所有菜单 view 权限
INSERT INTO t_sys_role_permission (role_id, menu_id, permission_code)
SELECT '00000000000000000000000000000010', menu_id, 'view'
FROM t_sys_menu;

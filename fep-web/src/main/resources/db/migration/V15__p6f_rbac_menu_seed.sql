-- V15: P6f TLQ 节点管理菜单种子数据
-- 在已有"TLQ 节点管理"父菜单（ID=10000000000000000000000000000007）下插入 3 个子菜单
-- 并为超管角色（ID=00000000000000000000000000000010）授予 view 权限
-- PRD §5.7
-- 注: 审计人员不授予 TLQ 菜单权限（PRD §5.9.1: TLQ 操作仅限系统管理员）

INSERT INTO t_sys_menu (menu_id, menu_code, menu_name, parent_id, menu_level, menu_icon, sort_order, menu_status, route_path) VALUES
('20000000000000000000000000000024', 'TLQ_NODE_CFG',  '节点配置',   '10000000000000000000000000000007', 2, 'server',    1, 'ACTIVE', '/tlq/nodes'),
('20000000000000000000000000000025', 'TLQ_QUEUE_CFG', '队列配置',   '10000000000000000000000000000007', 2, 'queue',     2, 'ACTIVE', '/tlq/queues'),
('20000000000000000000000000000026', 'TLQ_CONN_TEST', '连通性测试', '10000000000000000000000000000007', 2, 'heartbeat', 3, 'ACTIVE', '/tlq/connectivity');

-- 超管角色授予 3 个子菜单 view 权限
INSERT INTO t_sys_role_permission (role_id, menu_id, permission_code)
SELECT '00000000000000000000000000000010', menu_id, 'view'
FROM t_sys_menu
WHERE menu_id IN (
    '20000000000000000000000000000024',
    '20000000000000000000000000000025',
    '20000000000000000000000000000026'
);

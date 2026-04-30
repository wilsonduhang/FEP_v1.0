-- V21__seed_dir_map_menu.sql
-- P3a FR-MSG-DIR-MAP-CONFIG: register menu entry under SYS_CONFIG (parent menu_id =
-- 20000000000000000000000000000007, see V3 line 28).
-- Grant view+edit to super-admin role only (role_id =
-- 00000000000000000000000000000010, see V2 seed).

INSERT INTO t_sys_menu (menu_id, menu_code, menu_name, parent_id, menu_level, menu_icon, sort_order, menu_status, route_path) VALUES
('30000000000000000000000000000011', 'SYS_DIR_MAP', '报文方向映射', '20000000000000000000000000000007', 3, 'route', 100, 'ACTIVE', '/system/config/dir-map');

INSERT INTO t_sys_role_permission (role_id, menu_id, permission_code) VALUES
('00000000000000000000000000000010', '30000000000000000000000000000011', 'view'),
('00000000000000000000000000000010', '30000000000000000000000000000011', 'edit');

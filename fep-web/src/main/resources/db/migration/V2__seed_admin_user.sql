-- 超管账号 + 系统管理员角色种子数据
-- 初始密码: admin@FEP2026 (BCrypt strength=12)
-- 首次登录强制修改密码（由应用逻辑处理，检查 last_password_change_time IS NULL）
--
-- NOTE: user_account = 'admin1' (6 chars) to satisfy LoginRequest @Size(min=6)
-- per PRD §5.1.3. Fixed during P7.1 E2E smoke test (原 'admin' 5 chars 违反 PRD).

-- 固定 UUID 方便种子稳定
-- admin user: 00000000000000000000000000000001
-- SYSTEM_ADMIN role: 00000000000000000000000000000010

INSERT INTO t_sys_user (
    user_id, user_account, user_name, password_hash,
    user_status, last_password_change_time, create_by, update_by
) VALUES (
    '00000000000000000000000000000001',
    'admin1',
    '系统管理员',
    '$2a$12$5W/KCtgNI3U7YGhI9hgEMO2D4OAOyGLSkAqvJuVv94aQvce4EqK2q',
    'ACTIVE',
    NULL,
    'system',
    'system'
);

INSERT INTO t_sys_role (
    role_id, role_code, role_name, role_type, role_status, remark, create_by, update_by
) VALUES (
    '00000000000000000000000000000010',
    'SYSTEM_ADMIN',
    '系统管理员',
    'SYSTEM',
    'ACTIVE',
    '系统内置超级管理员角色，拥有所有权限',
    'system',
    'system'
);

INSERT INTO t_sys_user_role (user_id, role_id) VALUES (
    '00000000000000000000000000000001',
    '00000000000000000000000000000010'
);

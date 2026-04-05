-- FEP 系统管理核心表 V1
-- 参见 PRD v1.3 §6.4 + P6a.1 决策（字段补充）
-- 兼容 H2 (MODE=MySQL) 和 MySQL 8.x

-- ========================================
-- t_sys_user: 用户信息表
-- ========================================
CREATE TABLE t_sys_user (
    user_id                     VARCHAR(32)   NOT NULL COMMENT '用户唯一标识 UUID',
    user_account                VARCHAR(50)   NOT NULL COMMENT '登录账号',
    user_name                   VARCHAR(100)  NOT NULL COMMENT '用户姓名',
    password_hash               VARCHAR(255)  NOT NULL COMMENT '密码 BCrypt 散列',
    phone                       VARCHAR(20)   DEFAULT NULL COMMENT '手机号',
    email                       VARCHAR(100)  DEFAULT NULL COMMENT '邮箱',
    department                  VARCHAR(100)  DEFAULT NULL COMMENT '所属部门',
    user_status                 VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/LOCKED/DISABLED',
    locked_until                TIMESTAMP     DEFAULT NULL COMMENT '锁定到期时间',
    login_fail_count            INT           NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    last_login_time             TIMESTAMP     DEFAULT NULL COMMENT '最后登录时间',
    last_password_change_time   TIMESTAMP     DEFAULT NULL COMMENT '密码最近变更时间',
    password_history            VARCHAR(1024) DEFAULT NULL COMMENT '最近三次密码散列（JSON 数组）',
    create_time                 TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by                   VARCHAR(50)   DEFAULT NULL COMMENT '创建人',
    update_time                 TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    update_by                   VARCHAR(50)   DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (user_id)
) COMMENT '用户信息表';

CREATE UNIQUE INDEX uk_sys_user_account ON t_sys_user (user_account);
CREATE INDEX idx_sys_user_status ON t_sys_user (user_status);
CREATE INDEX idx_sys_user_phone ON t_sys_user (phone);

-- ========================================
-- t_sys_role: 角色信息表
-- ========================================
CREATE TABLE t_sys_role (
    role_id         VARCHAR(32)   NOT NULL COMMENT '角色唯一标识 UUID',
    role_code       VARCHAR(50)   NOT NULL COMMENT '角色编码',
    role_name       VARCHAR(100)  NOT NULL COMMENT '角色名称',
    role_type       VARCHAR(20)   NOT NULL DEFAULT 'BUSINESS' COMMENT 'SYSTEM/BUSINESS/CUSTOM',
    data_scope      VARCHAR(500)  DEFAULT NULL COMMENT '数据权限范围',
    role_status     VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    remark          VARCHAR(500)  DEFAULT NULL COMMENT '备注',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by       VARCHAR(50)   DEFAULT NULL,
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(50)   DEFAULT NULL,
    PRIMARY KEY (role_id)
) COMMENT '角色信息表';

CREATE UNIQUE INDEX uk_sys_role_code ON t_sys_role (role_code);
CREATE INDEX idx_sys_role_status ON t_sys_role (role_status);

-- ========================================
-- t_sys_menu: 菜单配置表
-- ========================================
CREATE TABLE t_sys_menu (
    menu_id          VARCHAR(32)   NOT NULL COMMENT '菜单唯一标识 UUID',
    menu_code        VARCHAR(50)   NOT NULL COMMENT '菜单编码',
    menu_name        VARCHAR(100)  NOT NULL COMMENT '菜单名称',
    parent_id        VARCHAR(32)   DEFAULT NULL COMMENT '父级菜单 ID（顶级为 NULL）',
    menu_level       INT           NOT NULL DEFAULT 1 COMMENT '菜单层级 1-3',
    menu_icon        VARCHAR(100)  DEFAULT NULL COMMENT '菜单图标 class',
    sort_order       INT           NOT NULL DEFAULT 1 COMMENT '排序',
    menu_status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    component_path   VARCHAR(200)  DEFAULT NULL COMMENT '前端组件路径',
    route_path       VARCHAR(200)  DEFAULT NULL COMMENT '前端路由路径',
    create_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (menu_id)
) COMMENT '菜单配置表';

CREATE UNIQUE INDEX uk_sys_menu_code ON t_sys_menu (menu_code);
CREATE INDEX idx_sys_menu_parent ON t_sys_menu (parent_id);
CREATE INDEX idx_sys_menu_status ON t_sys_menu (menu_status);

-- ========================================
-- t_sys_user_role: 用户-角色关联表
-- ========================================
CREATE TABLE t_sys_user_role (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id         VARCHAR(32)   NOT NULL COMMENT '用户 ID',
    role_id         VARCHAR(32)   NOT NULL COMMENT '角色 ID',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) COMMENT '用户-角色关联表';

CREATE UNIQUE INDEX uk_sys_user_role ON t_sys_user_role (user_id, role_id);
CREATE INDEX idx_sys_user_role_role ON t_sys_user_role (role_id);

-- ========================================
-- t_sys_role_permission: 角色-权限关联表
-- ========================================
CREATE TABLE t_sys_role_permission (
    id                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_id             VARCHAR(32)   NOT NULL COMMENT '角色 ID',
    menu_id             VARCHAR(32)   NOT NULL COMMENT '菜单 ID',
    permission_code     VARCHAR(100)  NOT NULL COMMENT '权限码（view/add/edit/delete 等）',
    create_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) COMMENT '角色-权限关联表';

CREATE UNIQUE INDEX uk_sys_role_perm ON t_sys_role_permission (role_id, menu_id, permission_code);
CREATE INDEX idx_sys_role_perm_role ON t_sys_role_permission (role_id);
CREATE INDEX idx_sys_role_perm_menu ON t_sys_role_permission (menu_id);

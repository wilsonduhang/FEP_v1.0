-- FEP P6a.3 系统配置管理表
-- 参见 PRD v1.3 §5.10.7 / §6.4
-- 兼容 H2 (MODE=MySQL) 和 MySQL 8.x

-- t_sys_config: 通用系统配置表 (key-value)
CREATE TABLE t_sys_config (
    config_id       VARCHAR(32)   NOT NULL COMMENT '配置唯一标识 UUID',
    config_group    VARCHAR(50)   NOT NULL COMMENT '配置分组 (PLATFORM/SYSTEM/CERT)',
    config_key      VARCHAR(100)  NOT NULL COMMENT '配置键',
    config_value    TEXT          DEFAULT NULL COMMENT '配置值',
    config_desc     VARCHAR(200)  DEFAULT NULL COMMENT '配置描述',
    sort_order      INT           NOT NULL DEFAULT 0 COMMENT '排序',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (config_id)
) COMMENT '通用系统配置表';
CREATE UNIQUE INDEX uk_config_group_key ON t_sys_config (config_group, config_key);
CREATE INDEX idx_config_group ON t_sys_config (config_group);

-- t_sys_business_type: 业务类型表
CREATE TABLE t_sys_business_type (
    type_id         VARCHAR(32)   NOT NULL COMMENT '业务类型唯一标识 UUID',
    type_name       VARCHAR(100)  NOT NULL COMMENT '业务类型名称',
    type_code       VARCHAR(50)   NOT NULL COMMENT '业务类型编码（唯一）',
    sort_order      INT           NOT NULL DEFAULT 0 COMMENT '排序',
    type_status     VARCHAR(20)   NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (type_id)
) COMMENT '业务类型表';
CREATE UNIQUE INDEX uk_business_type_code ON t_sys_business_type (type_code);

-- t_sys_data_receiver: 数据接收方表
CREATE TABLE t_sys_data_receiver (
    receiver_id     VARCHAR(32)   NOT NULL COMMENT '接收方唯一标识 UUID',
    receiver_name   VARCHAR(100)  NOT NULL COMMENT '接收方名称',
    receiver_method VARCHAR(20)   NOT NULL COMMENT 'INTERFACE/FILE/FTP',
    receiver_address VARCHAR(500) DEFAULT NULL COMMENT '接收地址',
    receiver_status VARCHAR(20)   NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (receiver_id)
) COMMENT '数据接收方表';

-- t_sys_push_interface: 推送接口表
CREATE TABLE t_sys_push_interface (
    interface_id    VARCHAR(32)   NOT NULL COMMENT '推送接口唯一标识 UUID',
    interface_name  VARCHAR(100)  NOT NULL COMMENT '接口名称',
    interface_url   VARCHAR(500)  NOT NULL COMMENT '接口 URL',
    push_method     VARCHAR(20)   NOT NULL DEFAULT 'AUTO' COMMENT 'AUTO/MANUAL',
    auth_type       VARCHAR(20)   NOT NULL DEFAULT 'NONE' COMMENT 'TOKEN/OAUTH2/NONE',
    timeout_seconds INT           NOT NULL DEFAULT 30 COMMENT '超时时间（秒）',
    retry_count     INT           NOT NULL DEFAULT 3 COMMENT '重试次数',
    business_type_id VARCHAR(32)  DEFAULT NULL COMMENT '关联业务类型 ID',
    last_push_time  TIMESTAMP     DEFAULT NULL COMMENT '最近推送时间',
    interface_status VARCHAR(20)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (interface_id)
) COMMENT '推送接口表';
CREATE INDEX idx_push_business_type ON t_sys_push_interface (business_type_id);

-- t_sys_alert_rule: 接口预警规则表（单条配置）
CREATE TABLE t_sys_alert_rule (
    rule_id         VARCHAR(32)   NOT NULL COMMENT '预警规则唯一标识 UUID',
    alert_enabled   BOOLEAN       NOT NULL DEFAULT FALSE COMMENT '是否启用预警',
    threshold       INT           NOT NULL DEFAULT 0 COMMENT '预警阈值',
    alert_email     VARCHAR(200)  DEFAULT NULL COMMENT '通知邮箱',
    notify_method   VARCHAR(20)   NOT NULL DEFAULT 'EMAIL' COMMENT 'EMAIL/IN_APP/SMS',
    alert_frequency VARCHAR(20)   NOT NULL DEFAULT 'REALTIME' COMMENT 'REALTIME/HOURLY/DAILY',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (rule_id)
) COMMENT '接口预警规则表';

-- t_sys_output_type: 输出类型表
CREATE TABLE t_sys_output_type (
    output_type_id  VARCHAR(32)   NOT NULL COMMENT '输出类型唯一标识 UUID',
    type_name       VARCHAR(100)  NOT NULL COMMENT '输出类型名称',
    type_code       VARCHAR(50)   NOT NULL COMMENT '输出类型编码（唯一）',
    type_status     VARCHAR(20)   NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (output_type_id)
) COMMENT '输出类型表';
CREATE UNIQUE INDEX uk_output_type_code ON t_sys_output_type (type_code);

-- t_sys_data_type_config: 数据类型表
CREATE TABLE t_sys_data_type_config (
    data_type_id    VARCHAR(32)   NOT NULL COMMENT '数据类型唯一标识 UUID',
    type_name       VARCHAR(100)  NOT NULL COMMENT '数据类型名称',
    type_code       VARCHAR(50)   NOT NULL COMMENT '数据类型编码（唯一）',
    type_status     VARCHAR(20)   NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (data_type_id)
) COMMENT '数据类型表';
CREATE UNIQUE INDEX uk_data_type_code ON t_sys_data_type_config (type_code);

-- t_sys_enterprise: 企业主体表
CREATE TABLE t_sys_enterprise (
    enterprise_id   VARCHAR(32)   NOT NULL COMMENT '企业主体唯一标识 UUID',
    enterprise_name VARCHAR(200)  NOT NULL COMMENT '企业名称',
    usci            VARCHAR(18)   NOT NULL COMMENT '统一社会信用代码（18位）',
    content_type    VARCHAR(100)  DEFAULT NULL COMMENT 'Content-Type',
    client_id       VARCHAR(100)  DEFAULT NULL COMMENT 'Client-Id',
    key_params      VARCHAR(500)  DEFAULT NULL COMMENT '密钥参数描述/引用路径',
    sign_file_path  VARCHAR(500)  DEFAULT NULL COMMENT '签名文件引用路径',
    audit_status    VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    biz_count       INT           NOT NULL DEFAULT 0 COMMENT '关联业务数量',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (enterprise_id)
) COMMENT '企业主体表';
CREATE UNIQUE INDEX uk_enterprise_usci ON t_sys_enterprise (usci);
CREATE INDEX idx_enterprise_audit ON t_sys_enterprise (audit_status);

-- t_sys_enterprise_biz: 企业业务信息关联表
CREATE TABLE t_sys_enterprise_biz (
    id               VARCHAR(32)   NOT NULL COMMENT '关联唯一标识 UUID',
    enterprise_id    VARCHAR(32)   NOT NULL COMMENT '企业主体 ID',
    business_type_id VARCHAR(32)   NOT NULL COMMENT '业务类型 ID',
    config_json      TEXT          DEFAULT NULL COMMENT '业务关联配置 JSON',
    status           VARCHAR(20)   NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    create_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) COMMENT '企业业务信息关联表';
CREATE INDEX idx_ent_biz_enterprise ON t_sys_enterprise_biz (enterprise_id);
CREATE INDEX idx_ent_biz_biztype ON t_sys_enterprise_biz (business_type_id);

-- t_sys_enterprise_query_config: 企业精准查询配置表
CREATE TABLE t_sys_enterprise_query_config (
    id               VARCHAR(32)   NOT NULL COMMENT '配置唯一标识 UUID',
    enterprise_id    VARCHAR(32)   NOT NULL COMMENT '企业主体 ID',
    query_type       VARCHAR(50)   NOT NULL COMMENT '查询类型',
    query_params     TEXT          DEFAULT NULL COMMENT '查询参数 JSON',
    status           VARCHAR(20)   NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    create_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) COMMENT '企业精准查询配置表';
CREATE INDEX idx_ent_qc_enterprise ON t_sys_enterprise_query_config (enterprise_id);

-- Seed: alert rule default
INSERT INTO t_sys_alert_rule (rule_id, alert_enabled, threshold, alert_email, notify_method, alert_frequency)
VALUES ('default_alert_rule_00000000001', FALSE, 0, NULL, 'EMAIL', 'REALTIME');

-- Seed: platform config defaults
INSERT INTO t_sys_config (config_id, config_group, config_key, config_value, config_desc, sort_order) VALUES
('cfg_platform_name_00000000001', 'PLATFORM', 'PLATFORM_NAME', 'FEP 综合前置平台', '平台名称', 1),
('cfg_platform_logo_00000000002', 'PLATFORM', 'PLATFORM_LOGO', NULL, '平台 LOGO 文件路径', 2),
('cfg_platform_addr_00000000003', 'PLATFORM', 'CONTACT_ADDRESS', '', '联系地址', 3),
('cfg_platform_phon_00000000004', 'PLATFORM', 'SERVICE_PHONE', '', '客服电话', 4),
('cfg_platform_qr_0000000000005', 'PLATFORM', 'ADMIN_QR_CODE', NULL, '管理员二维码文件路径', 5),
('cfg_platform_nodata_000000006', 'PLATFORM', 'NO_DATA_IMAGE', NULL, '暂无数据图文件路径', 6);

-- Seed: system config defaults
INSERT INTO t_sys_config (config_id, config_group, config_key, config_value, config_desc, sort_order) VALUES
('cfg_system_role_000000000001', 'SYSTEM', 'ACCESS_ROLE', 'BANK', '接入角色 (BANK/INFO_SERVICE)', 1),
('cfg_system_mode_000000000002', 'SYSTEM', 'PRODUCT_MODE', 'API', '产品模式 (API/WAREHOUSE)', 2),
('cfg_system_tlq_host_00000003', 'SYSTEM', 'TLQ_HOST', 'localhost', 'TLQ 主机地址', 3),
('cfg_system_tlq_port_00000004', 'SYSTEM', 'TLQ_PORT', '5678', 'TLQ 端口', 4),
('cfg_system_tlq_queue_0000005', 'SYSTEM', 'TLQ_QUEUE_NAME', 'FEP_DEFAULT', 'TLQ 队列名', 5),
('cfg_system_biz_rule_0000006', 'SYSTEM', 'BIZ_RULE_AUTO_RETRY', 'true', '业务规则：自动重试', 6),
('cfg_system_biz_rule_0000007', 'SYSTEM', 'BIZ_RULE_MAX_RETRY', '3', '业务规则：最大重试次数', 7);

-- Seed: cert metadata (⛔ actual cert content managed by security specialist)
INSERT INTO t_sys_config (config_id, config_group, config_key, config_value, config_desc, sort_order) VALUES
('cfg-cert-001', 'CERT', 'CERT_NAME', '', '证书名称', 1),
('cfg-cert-002', 'CERT', 'CERT_SUBJECT', '', '证书主题（CN）', 2),
('cfg-cert-003', 'CERT', 'CERT_ISSUER', '', '颁发机构', 3),
('cfg-cert-004', 'CERT', 'CERT_VALID_FROM', '', '有效期起始（YYYYMMDD）', 4),
('cfg-cert-005', 'CERT', 'CERT_VALID_TO', '', '有效期截止（YYYYMMDD）', 5),
('cfg-cert-006', 'CERT', 'CERT_STATUS', 'INACTIVE', '证书状态（ACTIVE/INACTIVE/REVOKED）', 6),
('cfg-cert-007', 'CERT', 'CERT_FILE_PATH', '', '证书文件路径', 7);

-- fep-web/src/main/resources/db/migration/V4__create_p6a2_tables.sql
-- FEP P6a.2 系统管理表: 消息 / 消息已读 / 下载任务 / 操作日志 / 帮助内容
-- 参见 PRD v1.3 §5.10.4-§5.10.8 / §6.4
-- 兼容 H2 (MODE=MySQL) 和 MySQL 8.x

-- ========================================
-- t_sys_message: 系统消息表
-- ========================================
CREATE TABLE t_sys_message (
    message_id      VARCHAR(32)   NOT NULL COMMENT '消息唯一标识 UUID',
    message_type    VARCHAR(20)   NOT NULL COMMENT 'SYSTEM_NOTICE/BIZ_REMINDER/ALERT/TODO_TASK',
    message_title   VARCHAR(200)  NOT NULL COMMENT '消息标题',
    message_content TEXT          NOT NULL COMMENT '消息正文',
    sender_id       VARCHAR(32)   NOT NULL COMMENT '发送者 ID (FK → t_sys_user)',
    receiver_type   VARCHAR(20)   NOT NULL COMMENT 'USER/ROLE/ALL',
    receiver_id     VARCHAR(32)   DEFAULT NULL COMMENT '接收者 ID (user_id 或 role_id，ALL 时为 null)',
    message_status  VARCHAR(20)   NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/DELETED（逻辑删除）',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (message_id)
) COMMENT '系统消息表';

CREATE INDEX idx_msg_type ON t_sys_message (message_type);
CREATE INDEX idx_msg_receiver ON t_sys_message (receiver_type, receiver_id);
CREATE INDEX idx_msg_create_time ON t_sys_message (create_time);

-- ========================================
-- t_sys_message_read: 消息已读追踪表
-- ========================================
CREATE TABLE t_sys_message_read (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    message_id      VARCHAR(32)   NOT NULL COMMENT '消息 ID (FK → t_sys_message)',
    user_id         VARCHAR(32)   NOT NULL COMMENT '用户 ID',
    read_time       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    PRIMARY KEY (id)
) COMMENT '消息已读追踪表';

CREATE UNIQUE INDEX uk_msg_read ON t_sys_message_read (message_id, user_id);
CREATE INDEX idx_msg_read_user ON t_sys_message_read (user_id);

-- ========================================
-- t_sys_download_task: 下载任务表
-- ========================================
CREATE TABLE t_sys_download_task (
    task_id         VARCHAR(32)   NOT NULL COMMENT '任务唯一标识 UUID',
    task_name       VARCHAR(200)  NOT NULL COMMENT '任务名称',
    task_type       VARCHAR(20)   NOT NULL COMMENT 'DATA_EXPORT/REPORT_GEN/LOG_DOWNLOAD',
    requester_id    VARCHAR(32)   NOT NULL COMMENT '发起人 ID (FK → t_sys_user)',
    file_name       VARCHAR(255)  DEFAULT NULL COMMENT '生成的文件名',
    file_path       VARCHAR(500)  DEFAULT NULL COMMENT '文件存储路径',
    file_size       BIGINT        DEFAULT NULL COMMENT '文件大小（字节）',
    task_progress   INT           NOT NULL DEFAULT 0 COMMENT '进度 0-100',
    task_status     VARCHAR(20)   NOT NULL DEFAULT 'WAITING' COMMENT 'WAITING/GENERATING/COMPLETED/FAILED/EXPIRED',
    failure_reason  VARCHAR(500)  DEFAULT NULL COMMENT '失败原因',
    expire_time     TIMESTAMP     DEFAULT NULL COMMENT '文件过期时间',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (task_id)
) COMMENT '下载任务表';

CREATE INDEX idx_dl_requester ON t_sys_download_task (requester_id);
CREATE INDEX idx_dl_status ON t_sys_download_task (task_status);
CREATE INDEX idx_dl_expire ON t_sys_download_task (expire_time);

-- ========================================
-- t_sys_operation_log: 操作日志表
-- ========================================
CREATE TABLE t_sys_operation_log (
    log_id          VARCHAR(32)   NOT NULL COMMENT '日志唯一标识 UUID',
    user_id         VARCHAR(32)   DEFAULT NULL COMMENT '操作人 ID',
    user_account    VARCHAR(50)   DEFAULT NULL COMMENT '操作人账号（快照）',
    module          VARCHAR(50)   NOT NULL COMMENT '功能模块',
    operation       VARCHAR(50)   NOT NULL COMMENT '操作类型',
    description     VARCHAR(500)  DEFAULT NULL COMMENT '操作描述',
    method          VARCHAR(10)   NOT NULL COMMENT 'HTTP 方法',
    request_url     VARCHAR(500)  NOT NULL COMMENT '请求 URL',
    request_params  TEXT          DEFAULT NULL COMMENT '请求参数（截断至 2000 字符）',
    response_status INT           NOT NULL COMMENT 'HTTP 响应状态码',
    ip_address      VARCHAR(45)   NOT NULL COMMENT '客户端 IP',
    duration_ms     BIGINT        NOT NULL COMMENT '耗时（毫秒）',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (log_id)
) COMMENT '操作日志表';

CREATE INDEX idx_log_user ON t_sys_operation_log (user_account);
CREATE INDEX idx_log_module ON t_sys_operation_log (module);
CREATE INDEX idx_log_time ON t_sys_operation_log (create_time);

-- ========================================
-- t_sys_help_content: 帮助面板内容表
-- ========================================
CREATE TABLE t_sys_help_content (
    help_id         VARCHAR(32)   NOT NULL COMMENT '帮助唯一标识 UUID',
    page_code       VARCHAR(50)   NOT NULL COMMENT '页面编码（如 sys-user / sys-role）',
    title           VARCHAR(200)  NOT NULL COMMENT '帮助标题',
    summary         VARCHAR(500)  NOT NULL COMMENT '简要描述',
    content         TEXT          NOT NULL COMMENT '详细内容',
    sort_order      INT           NOT NULL DEFAULT 1 COMMENT '排序',
    help_status     VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (help_id)
) COMMENT '帮助面板内容表';

CREATE INDEX idx_help_page ON t_sys_help_content (page_code, help_status);

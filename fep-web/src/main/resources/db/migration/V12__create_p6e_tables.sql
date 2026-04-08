-- P6e 首页数据看板 + 业务数据管理 — 4 张表
-- PRD §5.2.2 待办事项 + §5.2.4 快捷入口 + §5.3.1 报文管理 + §5.3.2 报文数据字典

-- 1. 首页待办事项表 (§5.2.2)
CREATE TABLE t_dashboard_todo (
    todo_id          VARCHAR(32)   NOT NULL,
    task_type        VARCHAR(50)   NOT NULL,
    title            VARCHAR(50)   NOT NULL,
    priority         VARCHAR(20)   NOT NULL DEFAULT 'MEDIUM',
    todo_status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    target_url       VARCHAR(500)  DEFAULT NULL,
    assigned_user_id VARCHAR(32)   DEFAULT NULL,
    deadline         TIMESTAMP     DEFAULT NULL,
    completed_time   TIMESTAMP     DEFAULT NULL,
    create_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (todo_id)
);

CREATE INDEX idx_dashboard_todo_user ON t_dashboard_todo (assigned_user_id);
CREATE INDEX idx_dashboard_todo_status ON t_dashboard_todo (todo_status);
CREATE INDEX idx_dashboard_todo_priority ON t_dashboard_todo (priority);
CREATE INDEX idx_dashboard_todo_deadline ON t_dashboard_todo (deadline);

-- 2. 首页快捷入口表 (§5.2.4)
CREATE TABLE t_dashboard_shortcut (
    shortcut_id  VARCHAR(32)   NOT NULL,
    user_id      VARCHAR(32)   NOT NULL,
    shortcut_name VARCHAR(50)  NOT NULL,
    target_url   VARCHAR(500)  NOT NULL,
    icon         VARCHAR(100)  DEFAULT NULL,
    sort_order   INT           NOT NULL DEFAULT 0,
    visible      BOOLEAN       NOT NULL DEFAULT TRUE,
    create_time  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (shortcut_id)
);

CREATE INDEX idx_dashboard_shortcut_user ON t_dashboard_shortcut (user_id);
CREATE UNIQUE INDEX uk_dashboard_shortcut_user_name ON t_dashboard_shortcut (user_id, shortcut_name);

-- 3. 报文类型定义表 (§5.3.1 + §5.3.2)
CREATE TABLE t_biz_message_definition (
    definition_id     VARCHAR(32)   NOT NULL,
    message_code      VARCHAR(10)   NOT NULL,
    message_name      VARCHAR(200)  NOT NULL,
    business_type_id  VARCHAR(32)   DEFAULT NULL,
    direction         VARCHAR(20)   NOT NULL DEFAULT 'OUTBOUND',
    field_count       INT           NOT NULL DEFAULT 0,
    field_summary     VARCHAR(2000) DEFAULT NULL,
    sample_xml        TEXT          DEFAULT NULL,
    definition_status VARCHAR(20)   NOT NULL DEFAULT 'ENABLED',
    sort_order        INT           NOT NULL DEFAULT 0,
    create_time       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (definition_id)
);

CREATE UNIQUE INDEX uk_biz_msg_def_code ON t_biz_message_definition (message_code);
CREATE INDEX idx_biz_msg_def_biz_type ON t_biz_message_definition (business_type_id);
CREATE INDEX idx_biz_msg_def_direction ON t_biz_message_definition (direction);

-- 4. 报文记录表 (§5.3.1 明细视图)
CREATE TABLE t_biz_message_record (
    record_id         VARCHAR(32)    NOT NULL,
    message_code      VARCHAR(10)    NOT NULL,
    serial_no         VARCHAR(50)    NOT NULL,
    sender_node       VARCHAR(20)    DEFAULT NULL,
    receiver_node     VARCHAR(20)    DEFAULT NULL,
    direction         VARCHAR(20)    NOT NULL DEFAULT 'OUTBOUND',
    process_status    VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    business_no       VARCHAR(100)   DEFAULT NULL,
    amount            DECIMAL(18,4)  DEFAULT NULL,
    xml_content       TEXT           DEFAULT NULL,
    entry_method      VARCHAR(20)    NOT NULL DEFAULT 'API',
    access_count      BIGINT         NOT NULL DEFAULT 0,
    error_message     VARCHAR(500)   DEFAULT NULL,
    process_time      TIMESTAMP      DEFAULT NULL,
    create_time       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (record_id)
);

CREATE UNIQUE INDEX uk_biz_msg_record_serial ON t_biz_message_record (serial_no);
CREATE INDEX idx_biz_msg_record_code ON t_biz_message_record (message_code);
CREATE INDEX idx_biz_msg_record_status ON t_biz_message_record (process_status);
CREATE INDEX idx_biz_msg_record_direction ON t_biz_message_record (direction);
CREATE INDEX idx_biz_msg_record_create_time ON t_biz_message_record (create_time);

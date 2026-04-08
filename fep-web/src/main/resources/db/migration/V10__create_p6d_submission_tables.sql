-- P6d 数据报送管理 — 4 张表
-- PRD §5.5.2 输出接口 + §5.5.3 数据源 + §5.5.4 业务场景 + §5.5.5/§5.6 报送记录

-- 1. 输出接口表 (§5.5.2)
CREATE TABLE t_sub_output_interface (
    interface_id     VARCHAR(32)   NOT NULL,
    interface_name   VARCHAR(100)  NOT NULL,
    interface_url    VARCHAR(500)  NOT NULL,
    business_type_id VARCHAR(32)   DEFAULT NULL,
    auth_type        VARCHAR(20)   NOT NULL DEFAULT 'NONE',
    timeout_seconds  INT           NOT NULL DEFAULT 30,
    retry_count      INT           NOT NULL DEFAULT 3,
    interface_status VARCHAR(20)   NOT NULL DEFAULT 'ENABLED',
    last_call_time   TIMESTAMP     DEFAULT NULL,
    call_count       BIGINT        NOT NULL DEFAULT 0,
    create_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (interface_id)
);

CREATE UNIQUE INDEX uk_sub_output_interface_name ON t_sub_output_interface (interface_name);
CREATE INDEX idx_sub_output_interface_status ON t_sub_output_interface (interface_status);
CREATE INDEX idx_sub_output_interface_biz_type ON t_sub_output_interface (business_type_id);

-- 2. 数据源表 (§5.5.3)
CREATE TABLE t_sub_data_source (
    source_id        VARCHAR(32)   NOT NULL,
    source_name      VARCHAR(100)  NOT NULL,
    logo_path        VARCHAR(500)  DEFAULT NULL,
    contact_address  VARCHAR(200)  NOT NULL,
    contact_phone    VARCHAR(20)   NOT NULL,
    push_enabled     BOOLEAN       NOT NULL DEFAULT FALSE,
    content_type     VARCHAR(100)  DEFAULT NULL,
    client_id        VARCHAR(100)  DEFAULT NULL,
    source_status    VARCHAR(20)   NOT NULL DEFAULT 'ENABLED',
    create_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (source_id)
);

CREATE UNIQUE INDEX uk_sub_data_source_name ON t_sub_data_source (source_name);

-- 3. 业务场景表 (§5.5.4)
CREATE TABLE t_sub_business_scene (
    scene_id              VARCHAR(32)   NOT NULL,
    scene_name            VARCHAR(100)  NOT NULL,
    business_type_id      VARCHAR(32)   NOT NULL,
    push_method           VARCHAR(20)   NOT NULL,
    import_template_path  VARCHAR(500)  DEFAULT NULL,
    request_url           VARCHAR(500)  NOT NULL,
    sort_order            INT           NOT NULL DEFAULT 0,
    scene_status          VARCHAR(20)   NOT NULL DEFAULT 'ENABLED',
    create_time           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (scene_id)
);

CREATE UNIQUE INDEX uk_sub_business_scene_name ON t_sub_business_scene (scene_name);
CREATE INDEX idx_sub_business_scene_biz_type ON t_sub_business_scene (business_type_id);

-- 4. 报送记录表 (§5.5.5 + §5.6.1-4)
CREATE TABLE t_sub_submission_record (
    record_id        VARCHAR(32)    NOT NULL,
    message_type     VARCHAR(10)    NOT NULL,
    message_name     VARCHAR(200)   NOT NULL,
    business_type_id VARCHAR(32)    DEFAULT NULL,
    submitter_name   VARCHAR(200)   DEFAULT NULL,
    business_no      VARCHAR(100)   DEFAULT NULL,
    amount           DECIMAL(18,4)  DEFAULT NULL,
    data_count       INT            NOT NULL DEFAULT 1,
    entry_method     VARCHAR(20)    NOT NULL,
    entry_by         VARCHAR(50)    DEFAULT NULL,
    push_status      VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    push_time        TIMESTAMP      DEFAULT NULL,
    error_message    VARCHAR(500)   DEFAULT NULL,
    sort_order       INT            NOT NULL DEFAULT 0,
    create_time      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (record_id)
);

CREATE INDEX idx_sub_record_msg_type ON t_sub_submission_record (message_type);
CREATE INDEX idx_sub_record_push_status ON t_sub_submission_record (push_status);
CREATE INDEX idx_sub_record_biz_type ON t_sub_submission_record (business_type_id);
CREATE INDEX idx_sub_record_create_time ON t_sub_submission_record (create_time);

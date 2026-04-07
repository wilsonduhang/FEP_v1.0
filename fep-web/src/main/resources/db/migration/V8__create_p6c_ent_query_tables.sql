-- FEP P6c 企业信息查询管理表
-- 参见 PRD v1.3 §5.4 (FR-WEB-ENT)
-- 兼容 H2 (MODE=MySQL) 和 MySQL 8.x

-- t_ent_query_task: 企业信息查询任务表
CREATE TABLE t_ent_query_task (
    task_id           VARCHAR(32)   NOT NULL COMMENT '查询任务 ID',
    enterprise_id     VARCHAR(32)   NOT NULL COMMENT '发起查询的企业 ID',
    query_type        VARCHAR(20)   NOT NULL COMMENT '查询类型: REALTIME/BATCH',
    usci              VARCHAR(18)   NOT NULL COMMENT '被查询企业 USCI',
    query_target_name VARCHAR(200)  DEFAULT NULL COMMENT '被查询企业名称',
    task_status       VARCHAR(20)   NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PROCESSING/COMPLETED/FAILED',
    message_id        VARCHAR(64)   DEFAULT NULL COMMENT '报文追踪 ID',
    batch_file_path   VARCHAR(500)  DEFAULT NULL COMMENT '批量查询文件路径',
    result_summary    TEXT          DEFAULT NULL COMMENT '查询结果摘要 JSON',
    error_message     VARCHAR(500)  DEFAULT NULL COMMENT '失败原因',
    create_time       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    complete_time     TIMESTAMP     DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (task_id)
) COMMENT '企业信息查询任务';
CREATE INDEX idx_ent_query_task_ent ON t_ent_query_task (enterprise_id);
CREATE INDEX idx_ent_query_task_usci ON t_ent_query_task (usci);
CREATE INDEX idx_ent_query_task_status ON t_ent_query_task (task_status);

-- t_ent_auth_letter: 企业信息查询授权书表
CREATE TABLE t_ent_auth_letter (
    letter_id         VARCHAR(32)   NOT NULL COMMENT '授权书 ID',
    enterprise_id     VARCHAR(32)   NOT NULL COMMENT '授权企业 ID',
    auth_type         VARCHAR(20)   NOT NULL COMMENT 'PAPER/ELECTRONIC',
    auth_scope        VARCHAR(500)  DEFAULT NULL COMMENT '授权范围描述',
    authorized_usci   VARCHAR(18)   NOT NULL COMMENT '被授权查询的 USCI',
    authorized_name   VARCHAR(200)  DEFAULT NULL COMMENT '被授权企业名称',
    file_path         VARCHAR(500)  DEFAULT NULL COMMENT '授权书文件路径',
    letter_status     VARCHAR(20)   NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED/ACKNOWLEDGED/REJECTED',
    message_id        VARCHAR(64)   DEFAULT NULL COMMENT '报文追踪 ID',
    submit_time       TIMESTAMP     DEFAULT NULL COMMENT '提交时间',
    ack_time          TIMESTAMP     DEFAULT NULL COMMENT '回执时间',
    reject_reason     VARCHAR(500)  DEFAULT NULL COMMENT '驳回原因',
    create_time       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (letter_id)
) COMMENT '企业信息查询授权书';
CREATE INDEX idx_ent_auth_letter_ent ON t_ent_auth_letter (enterprise_id);
CREATE INDEX idx_ent_auth_letter_status ON t_ent_auth_letter (letter_status);

-- t_ent_query_result: 企业信息查询结果表
CREATE TABLE t_ent_query_result (
    result_id         VARCHAR(32)   NOT NULL COMMENT '结果 ID',
    task_id           VARCHAR(32)   NOT NULL COMMENT '关联查询任务 ID',
    result_usci       VARCHAR(18)   NOT NULL COMMENT '结果对应 USCI',
    enterprise_name   VARCHAR(200)  DEFAULT NULL COMMENT '企业名称',
    result_data       TEXT          DEFAULT NULL COMMENT '完整结果 JSON',
    result_status     VARCHAR(20)   NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/ERROR',
    error_code        VARCHAR(20)   DEFAULT NULL COMMENT 'HNDEMP 错误码',
    error_message     VARCHAR(500)  DEFAULT NULL COMMENT '错误描述',
    create_time       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (result_id)
) COMMENT '企业信息查询结果';
CREATE INDEX idx_ent_query_result_task ON t_ent_query_result (task_id);
CREATE INDEX idx_ent_query_result_usci ON t_ent_query_result (result_usci);

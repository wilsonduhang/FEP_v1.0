-- V14: P6f TLQ 节点管理 — 3 张新表
-- PRD §5.7 + §3.1 TLQ 通信架构

-- 1. TLQ 节点配置表
CREATE TABLE t_tlq_node (
    node_id         VARCHAR(32)     NOT NULL,
    node_name       VARCHAR(100)    NOT NULL,
    node_role       VARCHAR(30)     NOT NULL,
    host_ip         VARCHAR(45)     NOT NULL,
    port            INT             NOT NULL,
    vip_address     VARCHAR(45),
    protocol        VARCHAR(20)     NOT NULL DEFAULT 'TCP',
    node_status     VARCHAR(20)     NOT NULL DEFAULT 'UNKNOWN',
    description     VARCHAR(500),
    last_heartbeat  TIMESTAMP,
    create_time     TIMESTAMP       NOT NULL,
    update_time     TIMESTAMP       NOT NULL,
    CONSTRAINT pk_tlq_node PRIMARY KEY (node_id),
    CONSTRAINT uk_tlq_node_name UNIQUE (node_name),
    CONSTRAINT uk_tlq_node_host_port UNIQUE (host_ip, port)
);

-- 2. TLQ 队列配置表
CREATE TABLE t_tlq_queue_config (
    queue_id        VARCHAR(32)     NOT NULL,
    node_id         VARCHAR(32)     NOT NULL,
    queue_name      VARCHAR(200)    NOT NULL,
    channel_type    VARCHAR(20)     NOT NULL,
    queue_type      VARCHAR(20)     NOT NULL,
    queue_status    VARCHAR(20)     NOT NULL DEFAULT 'ENABLED',
    description     VARCHAR(500),
    create_time     TIMESTAMP       NOT NULL,
    update_time     TIMESTAMP       NOT NULL,
    CONSTRAINT pk_tlq_queue_config PRIMARY KEY (queue_id),
    CONSTRAINT uk_tlq_queue_name UNIQUE (queue_name),
    CONSTRAINT fk_tlq_queue_node FOREIGN KEY (node_id) REFERENCES t_tlq_node(node_id)
);

-- 3. TLQ 连通性测试记录表
CREATE TABLE t_tlq_connectivity_record (
    record_id       VARCHAR(32)     NOT NULL,
    node_id         VARCHAR(32)     NOT NULL,
    test_time       TIMESTAMP       NOT NULL,
    test_result     VARCHAR(20)     NOT NULL,
    rtt_ms          INT,
    error_message   VARCHAR(500),
    triggered_by    VARCHAR(50)     NOT NULL DEFAULT 'MANUAL',
    create_time     TIMESTAMP       NOT NULL,
    CONSTRAINT pk_tlq_connectivity_record PRIMARY KEY (record_id),
    CONSTRAINT fk_tlq_conn_node FOREIGN KEY (node_id) REFERENCES t_tlq_node(node_id)
);

-- 索引
CREATE INDEX idx_tlq_node_status ON t_tlq_node(node_status);
CREATE INDEX idx_tlq_queue_node ON t_tlq_queue_config(node_id);
CREATE INDEX idx_tlq_queue_channel ON t_tlq_queue_config(channel_type);
CREATE INDEX idx_tlq_conn_node ON t_tlq_connectivity_record(node_id);
CREATE INDEX idx_tlq_conn_time ON t_tlq_connectivity_record(test_time);
CREATE INDEX idx_tlq_conn_result ON t_tlq_connectivity_record(test_result);

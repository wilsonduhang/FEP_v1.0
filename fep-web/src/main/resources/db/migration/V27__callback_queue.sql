-- fep-web/src/main/resources/db/migration/V27__callback_queue.sql
-- 接口模式回调持久队列（重启幸存，镜像 outbound_message_queue 模式）
CREATE TABLE callback_queue (
    queue_id            VARCHAR(32)  NOT NULL,
    idempotency_key     VARCHAR(64)  NOT NULL,
    target_interface_id VARCHAR(32)  NOT NULL,
    msg_no              VARCHAR(8)   NOT NULL,
    payload_json        TEXT         NOT NULL,
    status              VARCHAR(16)  NOT NULL,
    last_error          VARCHAR(500),
    create_time         TIMESTAMP    NOT NULL,
    update_time         TIMESTAMP    NOT NULL,
    CONSTRAINT pk_callback_queue PRIMARY KEY (queue_id),
    CONSTRAINT uk_callback_queue_idem UNIQUE (idempotency_key)
);
CREATE INDEX idx_callback_queue_status_ctime ON callback_queue (status, create_time);

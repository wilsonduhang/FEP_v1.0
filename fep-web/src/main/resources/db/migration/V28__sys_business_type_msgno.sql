-- fep-web/src/main/resources/db/migration/V28__sys_business_type_msgno.sql
-- 业务类型↔inbound msgNo 成员（接口模式回调配置驱动解析）
CREATE TABLE sys_business_type_msgno (
    id       VARCHAR(32)  NOT NULL,
    type_id  VARCHAR(32)  NOT NULL,
    msg_no   VARCHAR(8)   NOT NULL,
    CONSTRAINT pk_sys_business_type_msgno PRIMARY KEY (id),
    CONSTRAINT uk_sbtm_type_msg UNIQUE (type_id, msg_no)
);
CREATE INDEX idx_sbtm_msg_no ON sys_business_type_msgno (msg_no);

-- HNDEMP center node code ("A1000143000104") below: must match FepConstants.HNDEMP_NODE_CODE (R-2 2026-05-07).
-- P4 T9 IT seed — contract_register_inbox 行内 contract 数据源 + 5 行业务 sample
--
-- 表存在前提：H2 MODE=MySQL 模式（与 V22/V23 schema 同款），由 @Sql 在 Flyway
-- 完成 V1-V23 初始化之后单独执行；不进入 Flyway 受管 V 历史。
--
-- 字段对齐 ContractInfo3101 的 9 必填字段：
--   serial_no         (cursor 列；递增；driver SQL 用 :cursor < serial_no 取增量)
--   send_node_code    14-char 节点（IT 用 institutionCode 'B43010104B0001'）
--   des_node_code     14-char 节点（HNDEMP 中心 'A1000143000104'）
--   contract_no       6-50 char 合同号 (htSerialNo)
--   contract_type     业务名（≤30 chars，String0to30）
--   digital_seal      Boolean lexical "0" / "1"（XSD Boolean type）
--   contract_filename 5-100 char FileName
--   jfqy_name         2-50 char qyName
--   yfqy_name         2-50 char qyName
--
-- 注：cursor 列 serial_no 用字典序递增，IT 期望第一次 cursor=null/empty →
-- watermarkStore.get() returns Optional.empty() → adapter 用 initialWatermark
-- "1970-01-01T00:00:00Z"，由于 ASCII '0' < 'C' < 'S' < 'Z' < lowercase；
-- "SN-001" / "SN-002" 等递增字典序大于 "1970-..." 字面，故 SQL 比较 :cursor <
-- serial_no 第一次匹配全部 5 行。

CREATE TABLE IF NOT EXISTS contract_register_inbox (
    serial_no         VARCHAR(30)  NOT NULL,
    send_node_code    VARCHAR(14)  NOT NULL,
    des_node_code     VARCHAR(14)  NOT NULL,
    contract_no       VARCHAR(50)  NOT NULL,
    contract_type     VARCHAR(30)  NOT NULL,
    digital_seal      VARCHAR(1)   NOT NULL,
    contract_filename VARCHAR(100) NOT NULL,
    jfqy_name         VARCHAR(50)  NOT NULL,
    yfqy_name         VARCHAR(50)  NOT NULL,
    CONSTRAINT pk_contract_register_inbox PRIMARY KEY (serial_no)
);

-- Reset between test methods. The @Sql is executed BEFORE_TEST_METHOD before
-- the @BeforeEach hook, so a single seed SQL script must be idempotent across
-- repeated runs of the same class. DELETE-then-INSERT achieves that without
-- relying on PK conflict semantics or DROP TABLE (which would race the
-- adapter's first-time schema sniff).
DELETE FROM contract_register_inbox;

-- 5 sample rows — serial_no 递增，driver 用 :cursor < serial_no 增量取数。
-- 这里 send_node_code 与 institutionCode 保持一致（mapper 实际从 props 取该值，
-- 表里仍存以模拟真实行内 schema；mapper 优先 props 而非 row）。
INSERT INTO contract_register_inbox
    (serial_no, send_node_code, des_node_code, contract_no, contract_type,
     digital_seal, contract_filename, jfqy_name, yfqy_name)
VALUES
    ('SN2026050200000000000000000001', 'B43010104B0001', 'A1000143000104',
     'HT-2026050200001', '供应链融资合同', '1', 'contract-001.pdf',
     '湖南某某甲方企业一', '湖南某某乙方企业一'),
    ('SN2026050200000000000000000002', 'B43010104B0001', 'A1000143000104',
     'HT-2026050200002', '供应链融资合同', '0', 'contract-002.pdf',
     '湖南某某甲方企业二', '湖南某某乙方企业二'),
    ('SN2026050200000000000000000003', 'B43010104B0001', 'A1000143000104',
     'HT-2026050200003', '供应链融资合同', '1', 'contract-003.pdf',
     '湖南某某甲方企业三', '湖南某某乙方企业三'),
    ('SN2026050200000000000000000004', 'B43010104B0001', 'A1000143000104',
     'HT-2026050200004', '供应链融资合同', '0', 'contract-004.pdf',
     '湖南某某甲方企业四', '湖南某某乙方企业四'),
    ('SN2026050200000000000000000005', 'B43010104B0001', 'A1000143000104',
     'HT-2026050200005', '供应链融资合同', '1', 'contract-005.pdf',
     '湖南某某甲方企业五', '湖南某某乙方企业五');

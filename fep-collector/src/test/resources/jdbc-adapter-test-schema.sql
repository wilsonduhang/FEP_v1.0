-- P4 T2: JdbcCollectorAdapterTest H2 schema
-- 模拟"行内业务发票表"作为 JDBC 适配器源端
-- 单测专用 — 与生产 schema 无关
-- cursor_key: 20 位零填充字符串，让字典序与数值序一致（避免 BIGINT/TIMESTAMP toString 字典序陷阱）
DROP TABLE IF EXISTS biz_invoice;
CREATE TABLE biz_invoice (
    invoice_id BIGINT PRIMARY KEY,
    buyer_name VARCHAR(200),
    amount DECIMAL(18,2),
    created_at TIMESTAMP NOT NULL,
    cursor_key VARCHAR(32) NOT NULL
);

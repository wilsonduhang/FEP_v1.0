-- §6.4.1 FR-DATA-DB-01: corporate account info table (PRD v1.3 §1958).
-- Supports H2 MODE=MySQL (test) and MySQL 8.0+ (prod).
--
-- Source message: 3006 QyAccQueryReturn3006 (对公客户状态查询回执). Tracks one row per
-- corporate account (enterprise_id = qyAccCode, the 对公客户统一社会信用代码 USCI),
-- upserted on each status query回执 (latest verification wins).
--
-- Transparent deviations from PRD §1958:
--   1. enterprise_id ← qyAccCode (USCI per QyAccQueryReturn3006 Javadoc) — used as PK.
--   2. account_status stores the RAW HNDEMP return code (accReturnCode); semantic ENUM mapping
--      (正常/冻结/止付/销户) DEFERRED to the domain expert (see DEF-B2-2). Note AccReturnCode
--      errata补 4 值 — domain mapping must reconcile the value domain (see rule_master prescan redline).
--   3. account_number / opening_bank / account_type nullable DEFERRED — the 3006 回执 carries only
--      account name + code + status, none of these three fields (risk disclosure #2).
--   4. last_verification_time = event arrival time. status_memo ← accReturnMemo (extension).
--   5. serial_no / created_at / updated_at — non-PRD columns for traceability + audit.
--
-- F-level compliance: V1-V39 zero modification.

CREATE TABLE IF NOT EXISTS corporate_account_records (
    enterprise_id          VARCHAR(64)  NOT NULL,
    account_name           VARCHAR(100),
    account_number         VARCHAR(32),
    opening_bank           VARCHAR(100),
    account_type           VARCHAR(20),
    account_status         VARCHAR(20)  NOT NULL,
    status_memo            VARCHAR(200),
    last_verification_time TIMESTAMP    NOT NULL,
    serial_no              VARCHAR(64)  NOT NULL,
    created_at             TIMESTAMP    NOT NULL,
    updated_at             TIMESTAMP    NOT NULL,
    CONSTRAINT pk_corp_account PRIMARY KEY (enterprise_id)
);

CREATE INDEX IF NOT EXISTS idx_corp_account_status ON corporate_account_records (account_status);
CREATE INDEX IF NOT EXISTS idx_corp_account_serial ON corporate_account_records (serial_no);

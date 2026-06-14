-- §6.4.1 FR-DATA-DB-01: financing application result tracking table (PRD v1.3 §1945).
-- Supports H2 MODE=MySQL (test) and MySQL 8.0+ (prod).
--
-- Source message: 3009 RzReturnInfo3009 (融资结果登记). Tracks one row per financing
-- application (application_id = platApplyNo), updated as the application progresses
-- through phases (upsert keyed on application_id).
--
-- Transparent deviations from PRD §1945:
--   1. amounts DECIMAL(20,4) instead of PRD DECIMAL(15,2) — unified with reconciliation ADR-P2e-3.
--   2. approval_status stores the RAW HNDEMP phase code (rzPhaseCode); semantic ENUM mapping
--      (待审批/审批中/审批通过/审批驳回) DEFERRED to the domain expert (see DEF-B2-2).
--   3. enterprise_id nullable — 3009 carries no 融资企业 USCI; only core_enterprise_name (hxqyName)
--      is available. enterprise_id population DEFERRED (risk disclosure #2).
--   4. application_time / result_notice_time = event arrival time — 3009 carries no submit time
--      (that field lives in the 3105 application message, not the 3009 result).
--   5. core_enterprise_name / rzpz_no / serial_no / created_at / updated_at — non-PRD columns
--      for available business data + idempotency + audit (mirror reconciliation).
--
-- F-level compliance: V1-V38 zero modification.

CREATE TABLE IF NOT EXISTS financing_application_records (
    application_id       VARCHAR(64)   NOT NULL,
    enterprise_id        VARCHAR(20),
    core_enterprise_name VARCHAR(200),
    rzpz_no              VARCHAR(64),
    application_amount   DECIMAL(20,4),
    approval_amount      DECIMAL(20,4),
    application_time     TIMESTAMP     NOT NULL,
    approval_status      VARCHAR(20)   NOT NULL,
    result_notice_time   TIMESTAMP,
    reject_reason        VARCHAR(500),
    serial_no            VARCHAR(64)   NOT NULL,
    created_at           TIMESTAMP     NOT NULL,
    updated_at           TIMESTAMP     NOT NULL,
    CONSTRAINT pk_financing_app PRIMARY KEY (application_id)
);

CREATE INDEX IF NOT EXISTS idx_financing_app_status ON financing_application_records (approval_status);
CREATE INDEX IF NOT EXISTS idx_financing_app_serial ON financing_application_records (serial_no);

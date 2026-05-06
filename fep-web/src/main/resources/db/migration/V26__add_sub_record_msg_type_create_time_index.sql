-- V26: Add composite index (message_type, create_time) to symmetrize V17 design.
-- Scope: INDEX-ONLY. No table structure changes. No data migration.
-- Related: P7.2b Dashboard SQL perf #3 (Distribution time-window enablement, R4 Task 2).
-- Decision: D-3 = keep V10 idx_sub_record_msg_type single-column index alongside.
--
-- Idempotency strategy:
--   - H2 (dev/test): IF NOT EXISTS provides defense-in-depth alongside Flyway version table.
--   - MySQL 8.0+ (prod): WARNING — MySQL 8.0 will REJECT `CREATE INDEX IF NOT EXISTS` as a
--     parse-time syntax error. Direct re-execution against mysql client is UNSAFE.
--     Flyway, however, never re-applies a version already in flyway_schema_history,
--     so under normal Flyway-managed deployment V26 runs exactly once. NEVER run V26
--     manually via mysql client — always go through Flyway.
--
-- If MySQL parse error becomes a blocker (e.g., split migration per dialect):
--   - Move dialect-specific migrations to db/migration/{h2,mysql}/ subdirs and configure
--     spring.flyway.locations accordingly. P9 deployment 阶段 evaluate.

CREATE INDEX IF NOT EXISTS idx_sub_record_msg_type_create_time
    ON t_sub_submission_record (message_type, create_time);

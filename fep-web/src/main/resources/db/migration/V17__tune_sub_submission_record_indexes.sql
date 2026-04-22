-- V17: Add composite indexes to optimize dashboard trend/distribution queries.
-- Scope: INDEX-ONLY. No table structure changes. No data migration.
-- Related: P7.2b Dashboard SQL perf (Plan R Task 5, 2026-04-22).

-- Composite index for trend query: GROUP BY create_time with push_status filter
CREATE INDEX idx_sub_record_push_status_create_time
    ON t_sub_submission_record (push_status, create_time);

-- Composite index for distribution query: business_type_id aggregation with time range
CREATE INDEX idx_sub_record_biz_type_create_time
    ON t_sub_submission_record (business_type_id, create_time);

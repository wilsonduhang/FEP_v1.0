package com.puchain.fep.web.submission.dashboard;

import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.web.submission.dashboard.service.SubDashboardService;
import com.puchain.fep.web.submission.record.repository.SubSubmissionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance baseline test for the submission dashboard endpoints.
 *
 * <p>Task 2 scope: assert Flyway V26 composite index
 * {@code idx_sub_record_msg_type_create_time} exists on
 * {@code t_sub_submission_record} with column order
 * {@code (message_type, create_time)}.</p>
 *
 * <p>Task 3 scope: 10 000-row {@link JdbcTemplate#batchUpdate} seed plus
 * P95 latency assertion under {@value #P95_BUDGET_MS} ms for getDashboard,
 * getTrend(30), getDistribution(messageType, 90), and
 * getDistribution(businessType, 90).</p>
 *
 * <p>Seed is deterministic (seed = 42) and reused across @Test methods via
 * the {@code count() &gt;= SEED_ROWS} early-exit in {@link #seed()} —
 * Surefire defaults ({@code forkCount=1}, {@code reuseForks=true}) keep the
 * Spring TestContext (and therefore the H2 datasource) alive across
 * methods.</p>
 *
 * <p>Naming note: file ends in {@code Test.java} (not {@code IT.java}) because
 * the project's Surefire config uses default includes ({@code *Test.java} only)
 * and has no Failsafe plugin — {@code *IT.java} files are silently skipped
 * (CLAUDE.md known constraint, P2b-DEFECT-002 lesson). Plan named this
 * {@code SubDashboardServicePerfIT}; renamed to picked-up form during execution.</p>
 *
 * <p>Identifier casing note: the project sets H2
 * {@code DATABASE_TO_LOWER=TRUE}, so all identifiers in
 * {@code INFORMATION_SCHEMA} appear lower-case.</p>
 *
 * <p>H2 2.x schema note: {@code INFORMATION_SCHEMA.INDEXES} no longer
 * exposes {@code COLUMN_NAME} / {@code ORDINAL_POSITION}; those moved
 * to {@code INFORMATION_SCHEMA.INDEX_COLUMNS}. We join the two views.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
class SubDashboardServicePerfTest {

    private static final int SEED_ROWS = 10_000;
    private static final int WARMUP_RUNS = 5;
    private static final int MEASURE_RUNS = 20;
    /** PRD §8.1 NFR (响应 &lt;100 ms) × 2x noise factor for IT environment. */
    private static final long P95_BUDGET_MS = 200L;

    private static final String[] MSG_TYPES = {
            "0204", "1101", "2010", "2020", "2030", "3115",
            "5001", "5002", "5003", "5101", "5201", "5202"};
    private static final String[] BIZ_TYPES = {
            "BIZ01", "BIZ02", "BIZ03", "BIZ04", "BIZ05", "BIZ06", "BIZ07", "BIZ08"};
    private static final String[] ENTRY_METHODS = {"API_CALL", "MANUAL_ENTRY"};
    private static final String[] PUSH_STATUSES = {"PENDING", "PUSHING", "PUSHED", "FAILED"};
    private static final int SEED_DAYS_SPREAD = 180;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SubDashboardService dashboardService;

    @Autowired
    private SubSubmissionRecordRepository recordRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seed() {
        if (recordRepository.count() >= SEED_ROWS) {
            return;
        }
        jdbcTemplate.update("DELETE FROM t_sub_submission_record");

        final Random rng = new Random(42L);
        final LocalDateTime base = LocalDate.now()
                .minusDays(SEED_DAYS_SPREAD).atStartOfDay();
        final List<Object[]> batch = new ArrayList<>(SEED_ROWS);
        for (int i = 0; i < SEED_ROWS; i++) {
            final String recordId = IdGenerator.uuid32();
            final String messageType = MSG_TYPES[rng.nextInt(MSG_TYPES.length)];
            final String messageName = "TEST_MSG_" + messageType;
            final String businessTypeId = (rng.nextInt(10) == 0) ? null
                    : BIZ_TYPES[rng.nextInt(BIZ_TYPES.length)];
            final int dataCount = 1;
            final String entryMethod = ENTRY_METHODS[rng.nextInt(ENTRY_METHODS.length)];
            final String pushStatus = PUSH_STATUSES[rng.nextInt(PUSH_STATUSES.length)];
            final int sortOrder = i;
            final LocalDateTime createTime = base.plusMinutes(
                    rng.nextInt(SEED_DAYS_SPREAD * 24 * 60));
            batch.add(new Object[] {
                    recordId, messageType, messageName, businessTypeId,
                    dataCount, entryMethod, pushStatus, sortOrder,
                    Timestamp.valueOf(createTime), Timestamp.valueOf(createTime)
            });
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO t_sub_submission_record "
                        + "(record_id, message_type, message_name, business_type_id, "
                        + " data_count, entry_method, push_status, sort_order, "
                        + " create_time, update_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                batch);
    }

    @Test
    void v26AddsCompositeIndexOnMessageTypeCreateTime() throws Exception {
        final List<String> orderedColumns = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT IC.COLUMN_NAME "
                             + "FROM INFORMATION_SCHEMA.INDEXES I "
                             + "INNER JOIN INFORMATION_SCHEMA.INDEX_COLUMNS IC "
                             + "  ON I.INDEX_NAME = IC.INDEX_NAME "
                             + " AND I.TABLE_NAME = IC.TABLE_NAME "
                             + "WHERE I.TABLE_NAME = 't_sub_submission_record' "
                             + "  AND I.INDEX_NAME = 'idx_sub_record_msg_type_create_time' "
                             + "ORDER BY IC.ORDINAL_POSITION")) {
            while (rs.next()) {
                orderedColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
        }

        assertThat(orderedColumns)
                .as("V26 must declare composite index "
                        + "idx_sub_record_msg_type_create_time(message_type, create_time)")
                .containsExactly("message_type", "create_time");
    }

    @Test
    void getDashboardP95UnderBudgetAt10kRows() {
        final long p95 = measureP95(dashboardService::getDashboard);
        assertThat(p95)
                .as("getDashboard P95 ms over %d warmup + %d measure runs",
                        WARMUP_RUNS, MEASURE_RUNS)
                .isLessThanOrEqualTo(P95_BUDGET_MS);
    }

    @Test
    void getTrend30P95UnderBudgetAt10kRows() {
        final long p95 = measureP95(() -> dashboardService.getTrend(30));
        assertThat(p95)
                .as("getTrend(30) P95 ms over %d warmup + %d measure runs",
                        WARMUP_RUNS, MEASURE_RUNS)
                .isLessThanOrEqualTo(P95_BUDGET_MS);
    }

    @Test
    void getDistributionMessageTypeP95UnderBudgetAt10kRows() {
        final long p95 = measureP95(() ->
                dashboardService.getDistribution("messageType", 90));
        assertThat(p95)
                .as("getDistribution(messageType,90) P95 ms over %d warmup + %d measure runs",
                        WARMUP_RUNS, MEASURE_RUNS)
                .isLessThanOrEqualTo(P95_BUDGET_MS);
    }

    @Test
    void getDistributionBusinessTypeP95UnderBudgetAt10kRows() {
        final long p95 = measureP95(() ->
                dashboardService.getDistribution("businessType", 90));
        assertThat(p95)
                .as("getDistribution(businessType,90) P95 ms over %d warmup + %d measure runs",
                        WARMUP_RUNS, MEASURE_RUNS)
                .isLessThanOrEqualTo(P95_BUDGET_MS);
    }

    private long measureP95(final Runnable body) {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            body.run();
        }
        final long[] samples = new long[MEASURE_RUNS];
        for (int i = 0; i < MEASURE_RUNS; i++) {
            final long t0 = System.nanoTime();
            body.run();
            samples[i] = (System.nanoTime() - t0) / 1_000_000L;
        }
        Arrays.sort(samples);
        return samples[(int) Math.ceil(MEASURE_RUNS * 0.95) - 1];
    }
}

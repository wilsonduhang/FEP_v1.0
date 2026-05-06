package com.puchain.fep.web.submission.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance baseline test for the submission dashboard endpoints.
 *
 * <p>Task 2 scope: assert Flyway V26 composite index
 * {@code idx_sub_record_msg_type_create_time} exists on
 * {@code t_sub_submission_record} with column order
 * {@code (message_type, create_time)}.</p>
 *
 * <p>Task 3 will extend this test with 10K seed + P95 latency assertions.</p>
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

    @Autowired
    private DataSource dataSource;

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
}

package com.puchain.fep.web.outbound;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Schema regression test for Flyway V25 (P5 T1) — verifies that the three
 * observability columns ({@code sent_at} / {@code msg_id} / {@code tlq_send_result})
 * are added to {@code outbound_message_queue} and that the V22 retry index
 * {@code idx_outbound_queue_retry} is preserved (V25 must NOT recreate it —
 * V22 line 48 already has the equivalent {@code (status, next_retry_at)} index).
 *
 * <p>Boundary assertions also pin column data types and nullability against
 * {@code information_schema} to guard against accidental DDL drift in future
 * migrations.</p>
 *
 * <p>Uses {@code @SpringBootTest} (not {@code @JdbcTest}) to share the test-suite
 * ApplicationContext, matching the {@code V19V20DirMapMigrationTest} precedent —
 * {@code @JdbcTest} excludes JPA auto-config which fails because {@code FepApplication}
 * declares {@code @EnableJpaRepositories}.</p>
 *
 * @author FEP Team
 * @since 1.0.0 (P5 T1)
 */
@SpringBootTest
class V25SchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void v25_should_add_3_observability_columns() {
        final List<String> cols = jdbc.queryForList(
            "SELECT LOWER(column_name) FROM information_schema.columns "
                + "WHERE LOWER(table_name) = 'outbound_message_queue' "
                + "AND LOWER(column_name) IN ('sent_at', 'msg_id', 'tlq_send_result')",
            String.class);
        assertThat(cols).containsExactlyInAnyOrder("sent_at", "msg_id", "tlq_send_result");
    }

    @Test
    void v22_existing_index_status_next_retry_at_should_remain() {
        // v0.4 修订 F4: V22 行 48 已有等价索引 idx_outbound_queue_retry (status, next_retry_at)
        // V25 不重复创建（避免同列组冗余索引）。本测试退化为 schema regression 守护。
        final List<String> idx = jdbc.queryForList(
            "SELECT LOWER(index_name) FROM information_schema.indexes "
                + "WHERE LOWER(table_name) = 'outbound_message_queue' "
                + "AND LOWER(index_name) = 'idx_outbound_queue_retry'",
            String.class);
        assertThat(idx).hasSize(1);
    }

    @Test
    void v25_columns_should_be_nullable_with_correct_types() {
        // Boundary: pin column types + nullability so a future migration cannot
        // silently change VARCHAR(20) -> VARCHAR(64) or NULL -> NOT NULL.
        final List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT LOWER(column_name) AS col, data_type, is_nullable, character_maximum_length "
                + "FROM information_schema.columns "
                + "WHERE LOWER(table_name) = 'outbound_message_queue' "
                + "AND LOWER(column_name) IN ('sent_at', 'msg_id', 'tlq_send_result') "
                + "ORDER BY LOWER(column_name)");
        assertThat(rows).hasSize(3);

        // msg_id: VARCHAR(20), nullable
        final Map<String, Object> msgId = rows.stream()
            .filter(r -> "msg_id".equals(r.get("col"))).findFirst().orElseThrow();
        assertThat(((String) msgId.get("data_type")).toUpperCase()).contains("CHAR");
        assertThat(((String) msgId.get("is_nullable")).toUpperCase()).isEqualTo("YES");
        assertThat(((Number) msgId.get("character_maximum_length")).intValue()).isEqualTo(20);

        // tlq_send_result: VARCHAR(64), nullable
        final Map<String, Object> tlqRes = rows.stream()
            .filter(r -> "tlq_send_result".equals(r.get("col"))).findFirst().orElseThrow();
        assertThat(((String) tlqRes.get("data_type")).toUpperCase()).contains("CHAR");
        assertThat(((String) tlqRes.get("is_nullable")).toUpperCase()).isEqualTo("YES");
        assertThat(((Number) tlqRes.get("character_maximum_length")).intValue()).isEqualTo(64);

        // sent_at: TIMESTAMP, nullable
        final Map<String, Object> sentAt = rows.stream()
            .filter(r -> "sent_at".equals(r.get("col"))).findFirst().orElseThrow();
        assertThat(((String) sentAt.get("data_type")).toUpperCase()).contains("TIMESTAMP");
        assertThat(((String) sentAt.get("is_nullable")).toUpperCase()).isEqualTo("YES");
    }
}

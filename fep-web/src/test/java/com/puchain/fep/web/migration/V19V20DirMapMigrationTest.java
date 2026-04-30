package com.puchain.fep.web.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies V19 schema and V20 88-row seed for {@code t_dir_map_config}
 * (P3a FR-MSG-DIR-MAP-CONFIG).
 *
 * <p>Replaces the abandoned {@code spring-boot:run + sleep + kill} smoke
 * harness with deterministic JdbcTemplate assertions over the H2 MODE=MySQL
 * dev/test datasource. Full 88-row equality vs
 * {@code MessageDirectionMap.TABLE} is covered by T7 IT.</p>
 *
 * <p>Intentionally omits {@code @ActiveProfiles}: matches the V4–V9 migration
 * test pattern so this test shares the test-suite ApplicationContext instead of
 * forcing a separate boot (saves ~6 min per CI run).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
class V19V20DirMapMigrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void shouldApplyV19SchemaAndV20Seed() {
        // V19 schema applied: 2 tables created
        Integer tableCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
              + "WHERE LOWER(table_name) IN ('t_dir_map_config', 't_dir_map_config_history')",
                Integer.class);
        assertThat(tableCount).as("V19 创建 2 表").isEqualTo(2);

        // V20 seed: exactly 88 rows
        Integer rowCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_dir_map_config", Integer.class);
        assertThat(rowCount).as("V20 seed 88 条").isEqualTo(88);

        // 抽样校验 §4.6 row 1: 3000/ACCEPTING_ORG → OUTBOUND_ACTIVE
        String direction = jdbc.queryForObject(
                "SELECT direction FROM t_dir_map_config "
              + "WHERE message_type = '3000' AND access_role = 'ACCEPTING_ORG'",
                String.class);
        assertThat(direction).as("3000+ACCEPTING_ORG → OUTBOUND_ACTIVE")
                .isEqualTo("OUTBOUND_ACTIVE");

        // 抽样校验 §4.6 row 15: 3107/ACCEPTING_ORG → NOT_APPLICABLE
        String dir3107 = jdbc.queryForObject(
                "SELECT direction FROM t_dir_map_config "
              + "WHERE message_type = '3107' AND access_role = 'ACCEPTING_ORG'",
                String.class);
        assertThat(dir3107).as("3107+ACCEPTING_ORG → NOT_APPLICABLE")
                .isEqualTo("NOT_APPLICABLE");
    }
}

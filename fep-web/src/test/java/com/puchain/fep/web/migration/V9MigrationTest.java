package com.puchain.fep.web.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies V9 migration inserts P6c enterprise query menu seed data.
 *
 * <p>Uses {@code @SpringBootTest} for H2 MODE=MySQL COMMENT syntax
 * compatibility (consistent with V7MigrationTest).</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
class V9MigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldInsertEntQueryParentMenu() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_menu WHERE menu_name = '企业信息查询'",
                Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldInsertChildMenus() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_menu WHERE menu_name IN ('查询任务管理', '授权书管理')",
                Integer.class);
        assertThat(count).isEqualTo(2);
    }
}

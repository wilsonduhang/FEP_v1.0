package com.puchain.fep.web.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 V8 迁移脚本正确创建 P6c 企业信息查询管理所需的 3 张表。
 *
 * <p>使用 {@code @SpringBootTest} 而非 {@code @DataJpaTest}，因为 H2 MODE=MySQL
 * 的 COMMENT 语法需要完整应用上下文（与 V6MigrationTest 保持一致）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
class V8MigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void v8Migration_shouldCreateEntQueryTaskTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_ent_query_task",
                Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    void v8Migration_shouldCreateEntAuthLetterTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_ent_auth_letter",
                Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    void v8Migration_shouldCreateEntQueryResultTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_ent_query_result",
                Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }
}

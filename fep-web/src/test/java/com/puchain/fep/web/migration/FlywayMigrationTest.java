package com.puchain.fep.web.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Flyway V1-V17 迁移脚本验证。
 *
 * <p>验证核心表创建、种子数据插入，以及 V17 合成索引存在性。</p>
 */
@SpringBootTest
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void tSysUserTableShouldExist() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_user", Integer.class);
        assertNotNull(count);
    }

    @Test
    void tSysRoleTableShouldExist() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_role", Integer.class);
        assertNotNull(count);
    }

    @Test
    void tSysMenuTableShouldExist() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_menu", Integer.class);
        assertNotNull(count);
    }

    @Test
    void tSysUserRoleTableShouldExist() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_user_role", Integer.class);
        assertNotNull(count);
    }

    @Test
    void tSysRolePermissionTableShouldExist() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_role_permission", Integer.class);
        assertNotNull(count);
    }

    @Test
    void adminUserShouldBeSeeded() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_user WHERE user_account = 'admin1'", Integer.class);
        assertNotNull(count);
        assertEquals(1, count);
    }

    @Test
    void systemAdminRoleShouldBeSeeded() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_role WHERE role_code = 'SYSTEM_ADMIN'", Integer.class);
        assertEquals(1, count);
    }

    @Test
    void topLevelMenusShouldBeSeeded() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_sys_menu WHERE menu_level = 1", Integer.class);
        assertEquals(9, count);
    }

    @ParameterizedTest(name = "V17 index {0} should exist")
    @ValueSource(strings = {
            "IDX_SUB_RECORD_PUSH_STATUS_CREATE_TIME",
            "IDX_SUB_RECORD_BIZ_TYPE_CREATE_TIME"
    })
    void v17IndexesShouldExist(final String indexName) {
        Integer distinctCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT INDEX_NAME) FROM INFORMATION_SCHEMA.INDEXES "
                        + "WHERE UPPER(INDEX_NAME) = ?",
                Integer.class, indexName);
        assertNotNull(distinctCount);
        assertEquals(1, distinctCount);
    }
}

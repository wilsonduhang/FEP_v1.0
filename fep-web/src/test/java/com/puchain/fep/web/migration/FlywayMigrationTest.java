package com.puchain.fep.web.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Flyway V1-V3 迁移脚本验证。
 *
 * <p>验证 5 张核心表创建成功 + 种子数据正确插入。</p>
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

    @Test
    void v17PushStatusCreateTimeIndexShouldExist() {
        Integer distinctCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT INDEX_NAME) FROM INFORMATION_SCHEMA.INDEXES "
                        + "WHERE UPPER(INDEX_NAME) = 'IDX_SUB_RECORD_PUSH_STATUS_CREATE_TIME'",
                Integer.class);
        assertNotNull(distinctCount);
        assertEquals(1, distinctCount);
    }

    @Test
    void v17BizTypeCreateTimeIndexShouldExist() {
        Integer distinctCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT INDEX_NAME) FROM INFORMATION_SCHEMA.INDEXES "
                        + "WHERE UPPER(INDEX_NAME) = 'IDX_SUB_RECORD_BIZ_TYPE_CREATE_TIME'",
                Integer.class);
        assertNotNull(distinctCount);
        assertEquals(1, distinctCount);
    }
}

package com.puchain.fep.web.sysmgmt.role;

import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.sysmgmt.role.domain.RoleType;
import com.puchain.fep.web.sysmgmt.role.dto.RoleCreateRequest;
import com.puchain.fep.web.sysmgmt.role.dto.RoleResponse;
import com.puchain.fep.web.sysmgmt.role.service.SysRoleService;
import com.puchain.fep.common.domain.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SysRoleService 集成测试。
 *
 * <p>使用 H2 内存数据库 + Flyway 种子数据验证角色管理服务。</p>
 */
@SpringBootTest
@Transactional
class SysRoleServiceTest {

    /** 种子数据中的 SYSTEM_ADMIN 角色 ID。 */
    private static final String SYSTEM_ADMIN_ROLE_ID = "00000000000000000000000000000010";

    @Autowired
    private SysRoleService roleService;

    @Test
    void createRoleShouldPersist() {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setRoleCode("AUDITOR");
        request.setRoleName("审计员");
        request.setRoleType(RoleType.BUSINESS);
        request.setRemark("审计角色");

        RoleResponse response = roleService.create(request);

        assertNotNull(response.getRoleId());
        assertEquals("AUDITOR", response.getRoleCode());
        assertEquals("审计员", response.getRoleName());
        assertEquals(RoleType.BUSINESS, response.getRoleType());
    }

    @Test
    void createDuplicateRoleCodeShouldThrow() {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setRoleCode("SYSTEM_ADMIN");
        request.setRoleName("重复角色");
        request.setRoleType(RoleType.BUSINESS);

        FepBusinessException ex = assertThrows(FepBusinessException.class,
                () -> roleService.create(request));
        assertTrue(ex.getMessage().contains("角色编码已存在"));
    }

    @Test
    void deleteSystemRoleShouldThrow() {
        FepBusinessException ex = assertThrows(FepBusinessException.class,
                () -> roleService.delete(SYSTEM_ADMIN_ROLE_ID));
        assertTrue(ex.getMessage().contains("系统内置角色不允许删除"));
    }

    @Test
    void searchShouldReturnSeededSystemAdmin() {
        PageResult<RoleResponse> result = roleService.search("系统管理员", 1, 20);

        assertTrue(result.getTotal() >= 1);
        boolean found = result.getRecords().stream()
                .anyMatch(r -> "SYSTEM_ADMIN".equals(r.getRoleCode()));
        assertTrue(found, "应搜索到种子角色 SYSTEM_ADMIN");
    }
}

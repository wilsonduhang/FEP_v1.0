package com.puchain.fep.web.sysmgmt.user.service;

import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.sysmgmt.user.dto.UserCreateRequest;
import com.puchain.fep.web.sysmgmt.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SysUserService 集成测试。
 *
 * <p>使用 H2 内存数据库 + Flyway 种子数据验证用户管理服务。</p>
 */
@SpringBootTest
@Transactional
class SysUserServiceTest {

    /** 种子数据中超管用户 ID。 */
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";

    /** 种子数据中 SYSTEM_ADMIN 角色 ID。 */
    private static final String SYSTEM_ADMIN_ROLE_ID = "00000000000000000000000000000010";

    @Autowired
    private SysUserService userService;

    @Test
    void createUserWithRoleShouldPersist() {
        UserCreateRequest request = new UserCreateRequest();
        request.setAccount("testuser01");
        request.setUserName("测试用户");
        request.setInitialPassword("Test1234");
        request.setPhone("13800138000");
        request.setEmail("test@fep.com");
        request.setDepartment("测试部");
        request.setRoleIds(List.of(SYSTEM_ADMIN_ROLE_ID));

        UserResponse response = userService.create(request);

        assertNotNull(response.getUserId(), "用户 ID 应自动生成");
        assertEquals("testuser01", response.getUserAccount());
        assertEquals("测试用户", response.getUserName());
        assertEquals("138****8000", response.getPhone(), "手机号应已脱敏");
        assertEquals(1, response.getRoleCodes().size());
        assertEquals("SYSTEM_ADMIN", response.getRoleCodes().get(0));
    }

    @Test
    void createDuplicateAccountShouldThrow() {
        UserCreateRequest request = new UserCreateRequest();
        request.setAccount("admin");
        request.setUserName("重复用户");
        request.setInitialPassword("Test1234");

        FepBusinessException ex = assertThrows(FepBusinessException.class,
                () -> userService.create(request));
        assertTrue(ex.getMessage().contains("登录账号已存在"));
    }

    @Test
    void findAdminWithRoleShouldReturnSystemAdmin() {
        UserResponse response = userService.findById(ADMIN_USER_ID);

        assertEquals("admin", response.getUserAccount());
        assertEquals("系统管理员", response.getUserName());
        assertTrue(response.getRoleCodes().contains("SYSTEM_ADMIN"),
                "超管用户应拥有 SYSTEM_ADMIN 角色");
    }
}

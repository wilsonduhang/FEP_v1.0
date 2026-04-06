package com.puchain.fep.web.sysmgmt.user.repository;

import com.puchain.fep.web.sysmgmt.user.domain.SysUser;
import com.puchain.fep.web.sysmgmt.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SysUserRepository 集成测试。
 *
 * <p>使用 H2 内存数据库 + Flyway 种子数据验证 Repository 方法。</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@Transactional
class SysUserRepositoryTest {

    @Autowired
    private SysUserRepository repository;

    @Test
    void findByUserAccountShouldReturnSeededAdmin() {
        Optional<SysUser> admin = repository.findByUserAccount("admin");
        assertTrue(admin.isPresent());
        assertEquals("系统管理员", admin.get().getUserName());
        assertEquals(UserStatus.ACTIVE, admin.get().getUserStatus());
    }

    @Test
    void existsByUserAccountShouldReturnTrueForAdmin() {
        assertTrue(repository.existsByUserAccount("admin"));
        assertFalse(repository.existsByUserAccount("nonexistent"));
    }

    @Test
    void saveAndFindByIdShouldWork() {
        SysUser user = new SysUser();
        user.setUserId("testuser000000000000000000000001");
        user.setUserAccount("testuser1");
        user.setUserName("测试用户");
        user.setPasswordHash("$2a$12$dummy");
        user.setUserStatus(UserStatus.ACTIVE);
        user.setLoginFailCount(0);

        repository.save(user);

        Optional<SysUser> found = repository.findById("testuser000000000000000000000001");
        assertTrue(found.isPresent());
        assertEquals("testuser1", found.get().getUserAccount());
    }
}

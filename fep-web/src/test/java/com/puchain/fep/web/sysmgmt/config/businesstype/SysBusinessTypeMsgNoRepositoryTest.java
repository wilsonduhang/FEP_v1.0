package com.puchain.fep.web.sysmgmt.config.businesstype;

import com.puchain.fep.web.sysmgmt.config.businesstype.domain.SysBusinessTypeMsgNo;
import com.puchain.fep.web.sysmgmt.config.businesstype.repository.SysBusinessTypeMsgNoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SysBusinessTypeMsgNoRepository} 行为验证。
 *
 * <p>使用 {@code @SpringBootTest} 而非 {@code @DataJpaTest}，因为 H2 MODE=MySQL 的
 * DDL (COMMENT 语法) 需要完整 Flyway + 应用上下文（与 SubSubmissionRecordRepositoryTest
 * 和 V6/V7/V8MigrationTest 保持一致）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
class SysBusinessTypeMsgNoRepositoryTest {

    @Autowired
    private SysBusinessTypeMsgNoRepository repository;

    /**
     * 通过 msgNo 查询关联的 businessType id 列表，应返回所有匹配项。
     */
    @Test
    void findBusinessTypeIdsByMsgNo_shouldReturnAllMatchingTypeIds() {
        repository.save(new SysBusinessTypeMsgNo("bt-1", "2103"));
        repository.save(new SysBusinessTypeMsgNo("bt-2", "2103"));
        repository.save(new SysBusinessTypeMsgNo("bt-3", "2103"));
        List<String> ids = repository.findBusinessTypeIdsByMsgNo("2103");
        assertThat(ids).containsExactlyInAnyOrder("bt-1", "bt-2", "bt-3");
    }
}

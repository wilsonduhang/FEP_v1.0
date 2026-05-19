package com.puchain.fep.web.callback.repository;

import com.puchain.fep.web.callback.domain.CallbackQueueEntity;
import com.puchain.fep.web.callback.domain.CallbackQueueStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CallbackQueueRepository} 行为验证。
 *
 * <p>使用 {@code @SpringBootTest} 而非 {@code @DataJpaTest}，因为 H2 MODE=MySQL 的
 * DDL (COMMENT 语法) 需要完整 Flyway + 应用上下文（与 SysBusinessTypeMsgNoRepositoryTest
 * 和 SubSubmissionRecordRepositoryTest 保持一致）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
class CallbackQueueRepositoryTest {

    @Autowired
    private CallbackQueueRepository repository;

    @Test
    void enqueueAndQueryPending_shouldPersistAndReturnByCreateOrder() {
        CallbackQueueEntity e = CallbackQueueEntity.pending(
                "key-1", "if-1", "2103", "{\"code\":\"200\"}");
        repository.save(e);

        assertThat(repository.existsByIdempotencyKey("key-1")).isTrue();
        List<CallbackQueueEntity> pending =
                repository.findTop50ByStatusOrderByCreateTimeAsc(CallbackQueueStatus.PENDING);
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getIdempotencyKey()).isEqualTo("key-1");
    }
}

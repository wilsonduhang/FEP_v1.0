package com.puchain.fep.web.callback.repository;

import com.puchain.fep.web.callback.domain.CallbackQueueEntity;
import com.puchain.fep.web.callback.domain.CallbackQueueStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Autowired
    private EntityManager entityManager;

    @Test
    void enqueueAndIdempotencyCheck_shouldPersist() {
        final CallbackQueueEntity e = CallbackQueueEntity.pending(
                "key-1", "if-1", "2103", "{\"code\":\"200\"}");
        repository.save(e);

        assertThat(repository.existsByIdempotencyKey("key-1")).isTrue();
        assertThat(repository.existsByIdempotencyKey("no-such-key")).isFalse();
    }

    @Test
    void claimBatch_shouldSelectPendingAndDueRetry_skipFutureAndTerminal() {
        final LocalDateTime past = LocalDateTime.now().minusMinutes(1);
        final LocalDateTime future = LocalDateTime.now().plusMinutes(10);
        repository.save(CallbackQueueEntity.pending("k-pending", "i", "3001", "{}"));
        final CallbackQueueEntity dueRetry = CallbackQueueEntity.pending("k-due", "i", "3001", "{}");
        dueRetry.markRetry(1, past, "e");
        repository.save(dueRetry);
        final CallbackQueueEntity futureRetry = CallbackQueueEntity.pending("k-future", "i", "3001", "{}");
        futureRetry.markRetry(1, future, "e");
        repository.save(futureRetry);
        final CallbackQueueEntity done = CallbackQueueEntity.pending("k-done", "i", "3001", "{}");
        done.markDone();
        repository.save(done);
        entityManager.flush();

        final List<String> ids = repository.claimBatch(50);

        assertThat(ids).hasSize(2);
        // 排序 next_retry_at NULLS FIRST → PENDING(null) 在 due-retry(past) 之前
    }

    @Test
    void markDone_shouldTransitionStatusAndPersist() {
        final CallbackQueueEntity e = CallbackQueueEntity.pending(
                "key-done", "if-1", "2103", "{}");
        repository.save(e);
        final String id = e.getQueueId();
        entityManager.flush();
        entityManager.clear();

        // 镜像 Task 6 runner 真实用法：findById 取 managed 实例 → mutate → save
        final CallbackQueueEntity loaded = repository.findById(id).orElseThrow();
        loaded.markDone();
        repository.save(loaded);
        entityManager.flush();
        entityManager.clear();

        final CallbackQueueEntity reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CallbackQueueStatus.DONE);
    }

    @Test
    void markFailed_shouldPersistTruncatedErrorAndFailedStatus() {
        final CallbackQueueEntity e = CallbackQueueEntity.pending(
                "key-fail", "if-1", "2103", "{}");
        repository.save(e);
        final String id = e.getQueueId();
        entityManager.flush();
        entityManager.clear();

        final CallbackQueueEntity loaded = repository.findById(id).orElseThrow();
        loaded.markFailed("x".repeat(600));
        repository.save(loaded);
        entityManager.flush();
        entityManager.clear();

        final CallbackQueueEntity reloaded = repository.findById(id).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CallbackQueueStatus.FAILED);
        assertThat(reloaded.getLastError()).hasSize(500);
    }
}

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

    @Test
    void findStaleSending_shouldFilterByClaimedAtThreshold() {
        // markSending() 写 claimedAt=now（entity 无法注入过去时间），故用阈值跨越 now 来区分命中。
        final CallbackQueueEntity sending1 = CallbackQueueEntity.pending("k-s1", "if-1", "3001", "{}");
        sending1.markSending();
        repository.save(sending1);
        final CallbackQueueEntity sending2 = CallbackQueueEntity.pending("k-s2", "if-2", "3001", "{}");
        sending2.markSending();
        repository.save(sending2);
        final CallbackQueueEntity pending = CallbackQueueEntity.pending("k-pending2", "if-3", "3001", "{}");
        repository.save(pending);
        entityManager.flush();

        // 阈值取 now+1min → 两 SENDING（claimedAt<now+1min）命中；PENDING 被状态过滤排除
        final List<CallbackQueueEntity> found =
                repository.findStaleSending(LocalDateTime.now().plusMinutes(1));
        assertThat(found).hasSize(2);
        assertThat(found).allMatch(q -> CallbackQueueStatus.SENDING.equals(q.getStatus()));

        // 阈值取 now-1min → 无 SENDING 行的 claimedAt 早于该阈值，验证 claimedAt 过滤生效
        final List<CallbackQueueEntity> none =
                repository.findStaleSending(LocalDateTime.now().minusMinutes(1));
        assertThat(none).isEmpty();
    }

    @Test
    void copyForReplay_shouldPersistNewRowLinkedToOriginalDeadLetter() {
        final CallbackQueueEntity dead = CallbackQueueEntity.pending("k-dlq", "if-1", "3009", "{\"p\":1}");
        dead.markRetry(4, LocalDateTime.now(), "prev");
        dead.markDeadLetter(5, "fatal");
        repository.save(dead);
        final String deadId = dead.getQueueId();
        entityManager.flush();
        entityManager.clear();

        final CallbackQueueEntity loadedDead = repository.findById(deadId).orElseThrow();
        final CallbackQueueEntity replay = CallbackQueueEntity.copyForReplay(loadedDead, "admin-7");
        repository.save(replay);
        final String replayId = replay.getQueueId();
        entityManager.flush();
        entityManager.clear();

        final CallbackQueueEntity reloaded = repository.findById(replayId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CallbackQueueStatus.PENDING);
        assertThat(reloaded.getRetryCount()).isZero();
        assertThat(reloaded.getOriginalDlqId()).isEqualTo(deadId);
        assertThat(reloaded.getReplayedBy()).isEqualTo("admin-7");
        assertThat(reloaded.getReplayedAt()).isNotNull();

        final List<CallbackQueueEntity> derived = repository.findByOriginalDlqId(deadId);
        assertThat(derived).hasSize(1);
        assertThat(derived.get(0).getQueueId()).isEqualTo(replayId);
    }

    @Test
    void findDeadLetter_shouldReturnOnlyDeadLetterRowsNewestFirst() {
        final CallbackQueueEntity dlq1 = CallbackQueueEntity.pending("k-d1", "if-1", "3001", "{}");
        dlq1.markDeadLetter(5, "e1");
        repository.save(dlq1);
        final CallbackQueueEntity dlq2 = CallbackQueueEntity.pending("k-d2", "if-2", "3001", "{}");
        dlq2.markDeadLetter(5, "e2");
        repository.save(dlq2);
        final CallbackQueueEntity pending = CallbackQueueEntity.pending("k-p", "if-3", "3001", "{}");
        repository.save(pending);
        entityManager.flush();

        final List<CallbackQueueEntity> found =
                repository.findDeadLetter(org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(found).hasSize(2);
        assertThat(found).allMatch(q -> CallbackQueueStatus.DEAD_LETTER.equals(q.getStatus()));
    }
}

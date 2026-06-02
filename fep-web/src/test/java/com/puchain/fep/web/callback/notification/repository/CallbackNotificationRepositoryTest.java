package com.puchain.fep.web.callback.notification.repository;

import com.puchain.fep.web.callback.notification.domain.CallbackNotificationEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CallbackNotificationRepository} 行为验证。
 *
 * <p>使用 {@code @SpringBootTest}（与其他 callback repo 测试一致，完整 Flyway 上下文）。
 * {@code in_app_notification} 无 FK，测试无需 seed 父行。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
class CallbackNotificationRepositoryTest {

    @Autowired
    private CallbackNotificationRepository repository;

    @Test
    void findUnread_shouldReturnOnlyUnreadOrderedByCreateTimeDesc() {
        repository.save(CallbackNotificationEntity.of("admin-1", "CALLBACK_DLQ", "ERROR",
                "t1", "m1", "Q1", "CALLBACK_DLQ_ENTRY"));
        repository.save(CallbackNotificationEntity.of("admin-1", "CALLBACK_DLQ", "ERROR",
                "t2", "m2", "Q2", "CALLBACK_DLQ_ENTRY"));
        final CallbackNotificationEntity readOne = CallbackNotificationEntity.of("admin-1",
                "CALLBACK_DLQ", "ERROR", "t3", "m3", "Q3", "CALLBACK_DLQ_ENTRY");
        readOne.markRead();
        repository.save(readOne);

        final List<CallbackNotificationEntity> unread =
                repository.findByUserIdAndReadFalseOrderByCreateTimeDesc("admin-1");

        assertThat(unread).hasSize(2)
                .extracting(CallbackNotificationEntity::getTitle)
                .containsExactlyInAnyOrder("t1", "t2");
    }

    @Test
    void countUnread_shouldExcludeReadAndOtherUsers() {
        repository.save(CallbackNotificationEntity.of("admin-2", "CALLBACK_DLQ", "ERROR",
                "t1", "m1", "Q1", "CALLBACK_DLQ_ENTRY"));
        final CallbackNotificationEntity readOne = CallbackNotificationEntity.of("admin-2",
                "CALLBACK_DLQ", "ERROR", "t2", "m2", "Q2", "CALLBACK_DLQ_ENTRY");
        readOne.markRead();
        repository.save(readOne);
        repository.save(CallbackNotificationEntity.of("other-user", "CALLBACK_DLQ", "ERROR",
                "t3", "m3", "Q3", "CALLBACK_DLQ_ENTRY"));

        assertThat(repository.countByUserIdAndReadFalse("admin-2")).isEqualTo(1L);
    }

    @Test
    void markRead_shouldFlipFlagAndStampReadAt() {
        final CallbackNotificationEntity e = repository.saveAndFlush(CallbackNotificationEntity.of(
                "admin-3", "CALLBACK_DLQ", "ERROR", "t1", "m1", "Q1", "CALLBACK_DLQ_ENTRY"));
        e.markRead();
        repository.saveAndFlush(e);

        final CallbackNotificationEntity found = repository.findById(e.getNotificationId()).orElseThrow();
        assertThat(found.isRead()).isTrue();
        assertThat(found.getReadAt()).isNotNull();
    }
}

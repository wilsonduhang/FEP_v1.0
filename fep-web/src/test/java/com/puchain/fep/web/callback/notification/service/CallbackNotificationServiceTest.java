package com.puchain.fep.web.callback.notification.service;

import com.puchain.fep.web.callback.notification.domain.CallbackNotificationEntity;
import com.puchain.fep.web.callback.notification.dto.CallbackNotificationResponse;
import com.puchain.fep.web.callback.notification.repository.CallbackNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 单元测试 for {@link CallbackNotificationService}。
 *
 * <p>mock {@link CallbackNotificationRepository}，验证未读列表投影、未读计数，及标记已读的
 * 用户边界（仅本人通知生效，他人通知静默忽略防越权）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CallbackNotificationServiceTest {

    @Mock
    private CallbackNotificationRepository repo;
    @InjectMocks
    private CallbackNotificationService svc;

    private static CallbackNotificationEntity notif(final String userId) {
        return CallbackNotificationEntity.of(userId, "CALLBACK_DLQ", "ERROR",
                "回调死信 - IF-001", "queueId=Q1 msgNo=9001 retryCount=5 error=io timeout",
                "Q1", "CALLBACK_DLQ_ENTRY");
    }

    @Test
    void listUnreadReturnsProjectedResponses() {
        when(repo.findByUserIdAndReadFalseOrderByCreateTimeDesc("u1"))
                .thenReturn(List.of(notif("u1")));

        final List<CallbackNotificationResponse> result = svc.listUnread("u1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo("CALLBACK_DLQ");
        assertThat(result.get(0).refId()).isEqualTo("Q1");
        assertThat(result.get(0).read()).isFalse();
    }

    @Test
    void unreadCountDelegatesToRepository() {
        when(repo.countByUserIdAndReadFalse("u1")).thenReturn(3L);

        assertThat(svc.unreadCount("u1")).isEqualTo(3L);
    }

    @Test
    void markReadOwnNotificationFlipsReadFlag() {
        final CallbackNotificationEntity entity = notif("u1");
        when(repo.findById("N1")).thenReturn(Optional.of(entity));

        svc.markRead("N1", "u1");

        assertThat(entity.isRead()).isTrue();
        assertThat(entity.getReadAt()).isNotNull();
    }

    @Test
    void markReadOtherUsersNotificationIsNoOp() {
        final CallbackNotificationEntity entity = notif("u2");
        when(repo.findById("N1")).thenReturn(Optional.of(entity));

        svc.markRead("N1", "u1");

        assertThat(entity.isRead()).isFalse();
        assertThat(entity.getReadAt()).isNull();
    }
}

package com.puchain.fep.web.callback.notification.service;

import com.puchain.fep.web.callback.notification.domain.CallbackNotificationEntity;
import com.puchain.fep.web.callback.notification.dto.CallbackNotificationResponse;
import com.puchain.fep.web.callback.notification.repository.CallbackNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 站内通知服务（当前登录用户视角）：未读列表 / 未读数 / 标记已读。
 *
 * <p>所有操作以 {@code userId} 为边界，确保用户仅能查看与标记自己的通知。
 * 参见 PRD v1.3 §5.5.3 回调可靠性告警（FR-INFRA-CALLBACK-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class CallbackNotificationService {

    private final CallbackNotificationRepository repo;

    /**
     * 构造站内通知服务。
     *
     * @param repo 站内通知仓储
     */
    public CallbackNotificationService(final CallbackNotificationRepository repo) {
        this.repo = repo;
    }

    /**
     * 查询当前用户未读通知（最新优先）。
     *
     * @param userId 当前登录用户 id
     * @return 未读通知响应列表
     */
    @Transactional(readOnly = true)
    public List<CallbackNotificationResponse> listUnread(final String userId) {
        return repo.findByUserIdAndReadFalseOrderByCreateTimeDesc(userId).stream()
                .map(CallbackNotificationResponse::from).toList();
    }

    /**
     * 统计当前用户未读通知数。
     *
     * @param userId 当前登录用户 id
     * @return 未读数
     */
    @Transactional(readOnly = true)
    public long unreadCount(final String userId) {
        return repo.countByUserIdAndReadFalse(userId);
    }

    /**
     * 标记指定通知为已读（仅当该通知属于当前用户时生效，否则静默忽略，防越权）。
     *
     * @param notificationId 通知 id
     * @param userId         当前登录用户 id
     */
    @Transactional
    public void markRead(final String notificationId, final String userId) {
        repo.findById(notificationId)
                .filter(n -> userId.equals(n.getUserId()))
                .ifPresent(CallbackNotificationEntity::markRead);
    }
}

package com.puchain.fep.web.callback.notification.repository;

import com.puchain.fep.web.callback.notification.domain.CallbackNotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * {@link CallbackNotificationEntity} 持久化仓储。
 *
 * <p>支持按用户查未读列表 / 未读计数（顶部红点轮询 T12）/ 全量分页（站内信中心）。
 * 参见 PRD v1.3 §5.10.7.2d 告警（FR-INFRA-CALLBACK-IN-APP-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface CallbackNotificationRepository
        extends JpaRepository<CallbackNotificationEntity, String> {

    /**
     * 查某用户全部未读通知（按创建时间倒序，最新在前）。
     *
     * @param userId 用户 id
     * @return 未读通知列表，可能为空
     */
    List<CallbackNotificationEntity> findByUserIdAndReadFalseOrderByCreateTimeDesc(String userId);

    /**
     * 统计某用户未读通知数（顶部红点）。
     *
     * @param userId 用户 id
     * @return 未读数
     */
    long countByUserIdAndReadFalse(String userId);

    /**
     * 分页查某用户全部通知（按创建时间倒序，站内信中心）。
     *
     * @param userId   用户 id
     * @param pageable 分页参数
     * @return 通知分页
     */
    Page<CallbackNotificationEntity> findByUserIdOrderByCreateTimeDesc(String userId, Pageable pageable);
}

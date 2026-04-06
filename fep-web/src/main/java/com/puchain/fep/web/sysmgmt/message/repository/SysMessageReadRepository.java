package com.puchain.fep.web.sysmgmt.message.repository;

import com.puchain.fep.web.sysmgmt.message.domain.SysMessageRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 消息已读追踪 Repository。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface SysMessageReadRepository extends JpaRepository<SysMessageRead, Long> {

    /**
     * 检查某用户是否已阅读指定消息。
     *
     * @param messageId 消息 ID
     * @param userId    用户 ID
     * @return true 表示已读，false 表示未读
     */
    boolean existsByMessageIdAndUserId(String messageId, String userId);
}

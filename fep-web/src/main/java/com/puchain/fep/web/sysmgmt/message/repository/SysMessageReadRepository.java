package com.puchain.fep.web.sysmgmt.message.repository;

import com.puchain.fep.web.sysmgmt.message.domain.SysMessageRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Set;

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

    /**
     * 批量查询指定用户已读的消息 ID 集合。
     *
     * @param userId     用户 ID
     * @param messageIds 消息 ID 集合
     * @return 已读消息 ID 集合
     */
    @Query("SELECT r.messageId FROM SysMessageRead r WHERE r.userId = :userId AND r.messageId IN :messageIds")
    Set<String> findReadMessageIds(@Param("userId") String userId,
                                   @Param("messageIds") Collection<String> messageIds);
}

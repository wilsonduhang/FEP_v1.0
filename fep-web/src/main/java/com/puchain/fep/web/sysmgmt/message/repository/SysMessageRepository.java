package com.puchain.fep.web.sysmgmt.message.repository;

import com.puchain.fep.web.sysmgmt.message.domain.SysMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 系统消息 Repository。
 *
 * <p>提供消息可见性过滤查询与未读计数。消息可见性规则：
 * messageStatus=NORMAL 且（ALL 广播 OR 指定用户 OR 用户所属角色）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface SysMessageRepository extends JpaRepository<SysMessage, String> {

    /**
     * 查询指定用户可见的消息（分页）。
     *
     * <p>可见条件：messageStatus=NORMAL 且满足以下任一：
     * <ul>
     *   <li>receiverType=ALL（广播）</li>
     *   <li>receiverType=USER AND receiverId=userId</li>
     *   <li>receiverType=ROLE AND receiverId IN roleIds</li>
     * </ul>
     * 结果按 createTime 降序排列。</p>
     *
     * @param userId  当前用户 ID
     * @param roleIds 当前用户所属角色 ID 列表（无角色时传 {@code List.of("__NONE__")} 以避免空 IN 子句）
     * @param pageable 分页参数
     * @return 分页消息列表
     */
    @Query("SELECT m FROM SysMessage m WHERE m.messageStatus = 'NORMAL' "
            + "AND (m.receiverType = 'ALL' "
            + "  OR (m.receiverType = 'USER' AND m.receiverId = :userId) "
            + "  OR (m.receiverType = 'ROLE' AND m.receiverId IN :roleIds)) "
            + "ORDER BY m.createTime DESC")
    Page<SysMessage> findVisibleMessages(@Param("userId") String userId,
                                         @Param("roleIds") List<String> roleIds,
                                         Pageable pageable);

    /**
     * 统计指定用户可见且未读的消息数量。
     *
     * <p>未读定义：在 t_sys_message_read 表中无对应记录。</p>
     *
     * @param userId  当前用户 ID
     * @param roleIds 当前用户所属角色 ID 列表（无角色时传 {@code List.of("__NONE__")} 以避免空 IN 子句）
     * @return 未读消息数量
     */
    @Query("SELECT COUNT(m) FROM SysMessage m WHERE m.messageStatus = 'NORMAL' "
            + "AND (m.receiverType = 'ALL' "
            + "  OR (m.receiverType = 'USER' AND m.receiverId = :userId) "
            + "  OR (m.receiverType = 'ROLE' AND m.receiverId IN :roleIds)) "
            + "AND NOT EXISTS (SELECT 1 FROM SysMessageRead r "
            + "  WHERE r.messageId = m.messageId AND r.userId = :userId)")
    long countUnread(@Param("userId") String userId,
                     @Param("roleIds") List<String> roleIds);
}

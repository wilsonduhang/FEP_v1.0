package com.puchain.fep.web.sysmgmt.message.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.domain.PaginationHelper;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.web.sysmgmt.message.domain.MessageStatus;
import com.puchain.fep.web.sysmgmt.message.domain.ReceiverType;
import com.puchain.fep.web.sysmgmt.message.domain.SysMessage;
import com.puchain.fep.web.sysmgmt.message.domain.SysMessageRead;
import com.puchain.fep.web.sysmgmt.message.dto.MessageCreateRequest;
import com.puchain.fep.web.sysmgmt.message.dto.MessageResponse;
import com.puchain.fep.web.sysmgmt.message.repository.SysMessageReadRepository;
import com.puchain.fep.web.sysmgmt.message.repository.SysMessageRepository;
import com.puchain.fep.web.sysmgmt.rel.repository.SysUserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 消息管理服务。
 *
 * <p>提供消息发布、个人消息查询、已读标记、未读计数、管理员列表与逻辑删除功能。
 * 支持广播（ALL）、指定用户（USER）、指定角色（ROLE）三种投递方式。
 * 参见 PRD v1.3 §5.10.4 消息管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SysMessageService {

    private static final Logger log = LoggerFactory.getLogger(SysMessageService.class);

    /** 当用户无角色时用于替代空 IN 子句的占位符，数据库中不存在此 ID。 */
    private static final String NONE_ROLE_PLACEHOLDER = "__NONE__";

    private final SysMessageRepository messageRepository;
    private final SysMessageReadRepository messageReadRepository;
    private final SysUserRoleRepository userRoleRepository;

    /**
     * 构造 SysMessageService。
     *
     * @param messageRepository     消息 Repository
     * @param messageReadRepository 消息已读 Repository
     * @param userRoleRepository    用户-角色关联 Repository
     */
    public SysMessageService(final SysMessageRepository messageRepository,
                              final SysMessageReadRepository messageReadRepository,
                              final SysUserRoleRepository userRoleRepository) {
        this.messageRepository = messageRepository;
        this.messageReadRepository = messageReadRepository;
        this.userRoleRepository = userRoleRepository;
    }

    /**
     * 发布消息。
     *
     * <p>非 ALL 类型的消息必须提供 receiverId，否则抛出业务异常。</p>
     *
     * @param request  消息创建请求
     * @param senderId 发送者用户 ID
     * @return 已保存的消息响应 DTO
     * @throws FepBusinessException receiverType 非 ALL 且 receiverId 为空时
     */
    @Transactional
    public MessageResponse publish(final MessageCreateRequest request, final String senderId) {
        if (request.getReceiverType() != ReceiverType.ALL
                && (request.getReceiverId() == null || request.getReceiverId().isBlank())) {
            throw new FepBusinessException(FepErrorCode.PARAM_4001,
                    "receiverType 为 " + request.getReceiverType() + " 时 receiverId 不能为空");
        }

        SysMessage message = new SysMessage();
        message.setMessageId(IdGenerator.uuid32());
        message.setMessageType(request.getMessageType());
        message.setMessageTitle(request.getTitle());
        message.setMessageContent(request.getContent());
        message.setSenderId(senderId);
        message.setReceiverType(request.getReceiverType());
        message.setReceiverId(request.getReceiverType() == ReceiverType.ALL ? null : request.getReceiverId());
        message.setMessageStatus(MessageStatus.NORMAL);
        message.setCreateTime(LocalDateTime.now());

        SysMessage saved = messageRepository.save(message);
        log.info("Message published: id={}, type={}, receiverType={}", saved.getMessageId(),
                saved.getMessageType(), saved.getReceiverType());
        return MessageResponse.from(saved, false);
    }

    /**
     * 查询当前用户可见的消息列表（分页）。
     *
     * <p>可见性规则：ALL 广播 + 指定到本用户 + 指定到本用户所属角色。</p>
     *
     * @param userId   当前用户 ID
     * @param pageNum  页码（1-based）
     * @param pageSize 每页大小
     * @return 分页消息列表（含已读状态）
     */
    public PageResult<MessageResponse> myMessages(final String userId,
                                                   final int pageNum,
                                                   final int pageSize) {
        List<String> roleIds = resolveRoleIds(userId);
        Pageable pageable = PaginationHelper.pageable(pageNum, pageSize);
        Page<SysMessage> page = messageRepository.findVisibleMessages(userId, roleIds, pageable);

        List<String> messageIds = page.getContent().stream()
                .map(SysMessage::getMessageId)
                .toList();
        Set<String> readIds = messageIds.isEmpty()
                ? Set.of()
                : messageReadRepository.findReadMessageIds(userId, messageIds);

        return PageResult.from(page, pageNum, pageSize,
                m -> MessageResponse.from(m, readIds.contains(m.getMessageId())));
    }

    /**
     * 管理员消息列表（分页），仅返回状态为 NORMAL 的消息，按创建时间降序。
     *
     * @param pageNum  页码（1-based）
     * @param pageSize 每页大小
     * @return 分页消息列表（isRead 固定为 false，管理视图不展示已读状态）
     */
    public PageResult<MessageResponse> adminList(final int pageNum, final int pageSize) {
        Pageable pageable = PaginationHelper.pageable(pageNum, pageSize,
                Sort.by("createTime").descending());
        Page<SysMessage> page = messageRepository.findByMessageStatus(MessageStatus.NORMAL, pageable);

        return PageResult.from(page, pageNum, pageSize,
                m -> MessageResponse.from(m, false));
    }

    /**
     * 标记单条消息为已读。
     *
     * <p>若消息不存在则抛出业务异常；若已标记过则幂等跳过。</p>
     *
     * @param messageId 消息 ID
     * @param userId    当前用户 ID
     * @throws FepBusinessException 消息不存在时
     */
    @Transactional
    public void markRead(final String messageId, final String userId) {
        messageRepository.findById(messageId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "消息不存在: " + messageId));

        if (!messageReadRepository.existsByMessageIdAndUserId(messageId, userId)) {
            messageReadRepository.save(new SysMessageRead(messageId, userId));
            log.debug("Message marked as read: messageId={}, userId={}", messageId, userId);
        }
    }

    /**
     * 批量标记当前用户所有可见消息为已读。
     *
     * @param userId 当前用户 ID
     */
    @Transactional
    public void markAllRead(final String userId) {
        List<String> roleIds = resolveRoleIds(userId);
        Pageable unpaged = Pageable.unpaged();
        Page<SysMessage> page = messageRepository.findVisibleMessages(userId, roleIds, unpaged);

        List<String> allMessageIds = page.getContent().stream()
                .map(SysMessage::getMessageId)
                .toList();
        Set<String> alreadyRead = allMessageIds.isEmpty()
                ? Set.of()
                : messageReadRepository.findReadMessageIds(userId, allMessageIds);

        List<SysMessageRead> newReads = page.getContent().stream()
                .filter(m -> !alreadyRead.contains(m.getMessageId()))
                .map(m -> new SysMessageRead(m.getMessageId(), userId))
                .toList();
        messageReadRepository.saveAll(newReads);
        log.info("Mark all read: userId={}, markedCount={}", userId, newReads.size());
    }

    /**
     * 查询当前用户的未读消息数量。
     *
     * @param userId 当前用户 ID
     * @return 未读消息数量
     */
    public long unreadCount(final String userId) {
        List<String> roleIds = resolveRoleIds(userId);
        return messageRepository.countUnread(userId, roleIds);
    }

    /**
     * 逻辑删除消息（设置 messageStatus=DELETED）。
     *
     * <p>不物理删除数据库记录。消息不存在时抛出业务异常。</p>
     *
     * @param messageId 消息 ID
     * @throws FepBusinessException 消息不存在时
     */
    @Transactional
    public void delete(final String messageId) {
        SysMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "消息不存在: " + messageId));

        message.setMessageStatus(MessageStatus.DELETED);
        messageRepository.save(message);
        log.info("Message logically deleted: messageId={}", messageId);
    }

    /**
     * 获取用户的角色 ID 列表；若用户无角色，返回含占位符的列表以避免空 IN 子句。
     *
     * @param userId 用户 ID
     * @return 角色 ID 列表（至少含一个元素）
     */
    private List<String> resolveRoleIds(final String userId) {
        List<String> roleIds = userRoleRepository.findRoleIdsByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of(NONE_ROLE_PLACEHOLDER);
        }
        return roleIds;
    }
}

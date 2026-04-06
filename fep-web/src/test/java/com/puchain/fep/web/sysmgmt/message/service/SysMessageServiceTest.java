package com.puchain.fep.web.sysmgmt.message.service;

import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.sysmgmt.message.domain.MessageType;
import com.puchain.fep.web.sysmgmt.message.domain.ReceiverType;
import com.puchain.fep.web.sysmgmt.message.dto.MessageCreateRequest;
import com.puchain.fep.web.sysmgmt.message.dto.MessageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SysMessageService 集成测试。
 *
 * <p>使用 H2 内存数据库（dev profile）+ Flyway 迁移验证消息管理服务全流程：
 * 发布、查询、已读标记、未读计数、逻辑删除。
 * 参见 PRD v1.3 §5.10.4 消息管理。</p>
 */
@SpringBootTest
@Transactional
class SysMessageServiceTest {

    /** 种子数据中超管用户 ID。 */
    private static final String ADMIN_USER_ID = "00000000000000000000000000000001";

    /** 种子数据中 SYSTEM_ADMIN 角色 ID。 */
    private static final String SYSTEM_ADMIN_ROLE_ID = "00000000000000000000000000000010";

    /** 测试用普通用户 ID（不在种子数据中，用于隔离测试）。 */
    private static final String TEST_USER_ID = "ffffffffffffffffffffffffffffffff";

    @Autowired
    private SysMessageService messageService;

    /**
     * 测试1: 发布广播消息（ALL类型）应持久化并可被查询到。
     */
    @Test
    void publish_broadcastMessage_shouldPersist() {
        MessageCreateRequest req = buildRequest(MessageType.SYSTEM_NOTICE, ReceiverType.ALL, null);

        MessageResponse resp = messageService.publish(req, ADMIN_USER_ID);

        assertNotNull(resp.getMessageId(), "消息 ID 应自动生成");
        assertEquals(MessageType.SYSTEM_NOTICE, resp.getMessageType());
        assertEquals("测试标题", resp.getTitle());
        assertEquals(ReceiverType.ALL, resp.getReceiverType());
        assertFalse(resp.isRead(), "新消息默认未读");
    }

    /**
     * 测试2: 查询我的消息 — ALL 广播消息应对任意用户可见。
     */
    @Test
    void myMessages_shouldReturnMessagesForUser() {
        // 发布一条广播消息
        MessageCreateRequest req = buildRequest(MessageType.SYSTEM_NOTICE, ReceiverType.ALL, null);
        messageService.publish(req, ADMIN_USER_ID);

        PageResult<MessageResponse> result = messageService.myMessages(ADMIN_USER_ID, 1, 10);

        assertTrue(result.getTotal() >= 1, "应至少能看到一条广播消息");
        assertTrue(result.getRecords().stream()
                        .anyMatch(m -> m.getReceiverType() == ReceiverType.ALL),
                "结果中应包含 ALL 类型消息");
    }

    /**
     * 测试3: 标记消息已读 — 应记录已读时间并更新已读状态。
     */
    @Test
    void markRead_shouldRecordReadTime() {
        // 发布广播消息
        MessageCreateRequest req = buildRequest(MessageType.SYSTEM_NOTICE, ReceiverType.ALL, null);
        MessageResponse published = messageService.publish(req, ADMIN_USER_ID);
        String messageId = published.getMessageId();

        // 标记已读
        messageService.markRead(messageId, ADMIN_USER_ID);

        // 查询并验证已读状态
        PageResult<MessageResponse> result = messageService.myMessages(ADMIN_USER_ID, 1, 20);
        MessageResponse found = result.getRecords().stream()
                .filter(m -> messageId.equals(m.getMessageId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("找不到已发布的消息"));

        assertTrue(found.isRead(), "标记已读后 isRead 应为 true");
    }

    /**
     * 测试4: 未读计数 — 发布2条消息后未读数应为2。
     */
    @Test
    void unreadCount_shouldCountCorrectly() {
        // TEST_USER_ID 无角色，基准未读为 0
        long baseCount = messageService.unreadCount(TEST_USER_ID);

        // 发布2条广播消息
        messageService.publish(buildRequest(MessageType.ALERT, ReceiverType.ALL, null), ADMIN_USER_ID);
        messageService.publish(buildRequest(MessageType.BIZ_REMINDER, ReceiverType.ALL, null), ADMIN_USER_ID);

        long afterCount = messageService.unreadCount(TEST_USER_ID);

        assertEquals(baseCount + 2, afterCount, "发布2条广播消息后未读数应增加2");
    }

    /**
     * 测试5: USER 类型消息 — 只有目标用户可见。
     */
    @Test
    void publish_userMessage_shouldBeVisibleOnlyToTarget() {
        // 发布只给 ADMIN_USER_ID 的消息
        MessageCreateRequest req = buildRequest(MessageType.TODO_TASK, ReceiverType.USER, ADMIN_USER_ID);
        MessageResponse published = messageService.publish(req, ADMIN_USER_ID);

        assertEquals(ReceiverType.USER, published.getReceiverType());
        assertEquals(ADMIN_USER_ID, published.getReceiverId());

        // ADMIN_USER_ID 能看到
        PageResult<MessageResponse> adminResult = messageService.myMessages(ADMIN_USER_ID, 1, 20);
        assertTrue(adminResult.getRecords().stream()
                        .anyMatch(m -> published.getMessageId().equals(m.getMessageId())),
                "目标用户应能看到 USER 类型消息");

        // TEST_USER_ID（其他用户）看不到
        PageResult<MessageResponse> otherResult = messageService.myMessages(TEST_USER_ID, 1, 20);
        assertFalse(otherResult.getRecords().stream()
                        .anyMatch(m -> published.getMessageId().equals(m.getMessageId())),
                "非目标用户不应看到 USER 类型消息");
    }

    /**
     * 测试6: ROLE 类型消息 — 持久化后 receiverId 应为角色 ID。
     */
    @Test
    void publish_roleMessage_shouldPersistWithRoleReceiver() {
        MessageCreateRequest req = buildRequest(MessageType.BIZ_REMINDER, ReceiverType.ROLE, SYSTEM_ADMIN_ROLE_ID);
        MessageResponse resp = messageService.publish(req, ADMIN_USER_ID);

        assertEquals(ReceiverType.ROLE, resp.getReceiverType());
        assertEquals(SYSTEM_ADMIN_ROLE_ID, resp.getReceiverId(), "receiverId 应保存为角色 ID");
    }

    /**
     * 测试7: 逻辑删除 — adminList 中已删除的消息应被过滤掉。
     */
    @Test
    void delete_shouldLogicallyRemoveMessage() {
        // 发布并删除
        MessageResponse published = messageService.publish(
                buildRequest(MessageType.SYSTEM_NOTICE, ReceiverType.ALL, null), ADMIN_USER_ID);
        String messageId = published.getMessageId();

        messageService.delete(messageId);

        // adminList 中应不含已删除消息
        PageResult<MessageResponse> adminResult = messageService.adminList(1, 100);
        assertFalse(adminResult.getRecords().stream()
                        .anyMatch(m -> messageId.equals(m.getMessageId())),
                "逻辑删除后 adminList 不应返回该消息");
    }

    /**
     * 测试8: USER/ROLE 类型无 receiverId — 应抛出业务异常。
     */
    @Test
    void publish_nonAllWithoutReceiverId_shouldThrow() {
        MessageCreateRequest req = buildRequest(MessageType.ALERT, ReceiverType.USER, null);

        assertThrows(FepBusinessException.class,
                () -> messageService.publish(req, ADMIN_USER_ID),
                "USER 类型缺少 receiverId 应抛出 FepBusinessException");
    }

    // ===== Helper =====

    /**
     * 构建测试用消息创建请求。
     *
     * @param type         消息类型
     * @param receiverType 接收者类型
     * @param receiverId   接收者 ID（可为 null）
     * @return 消息创建请求
     */
    private MessageCreateRequest buildRequest(final MessageType type,
                                               final ReceiverType receiverType,
                                               final String receiverId) {
        MessageCreateRequest req = new MessageCreateRequest();
        req.setMessageType(type);
        req.setTitle("测试标题");
        req.setContent("测试消息内容");
        req.setReceiverType(receiverType);
        req.setReceiverId(receiverId);
        return req;
    }
}

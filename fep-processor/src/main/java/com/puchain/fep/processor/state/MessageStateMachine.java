package com.puchain.fep.processor.state;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.util.LogSanitizer;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * 报文级状态机。负责在 {@link MessageProcessStore} 之上守护 PRD v1.3 §4.7 模式1
 * 定义的 6 条合法状态转移，任何越界转换都会被 {@link IllegalMessageStateException} 拒绝。
 *
 * <p>合法路径：
 * <pre>
 * RECEIVED   → VALIDATED  | FAILED
 * VALIDATED  → PROCESSING | FAILED
 * PROCESSING → COMPLETED  | FAILED
 * COMPLETED  → (terminal, no outbound)
 * FAILED     → (terminal, no outbound)
 * </pre>
 *
 * <p>本组件不承担事务管理，数据一致性由底层 {@link MessageProcessStore}
 * 的实现（内存 / JPA）保证。异常消息中的 {@code transitionNo} 已通过
 * {@link LogSanitizer#sanitize} 清洗，避免日志注入。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class MessageStateMachine {

    private static final Map<MessageProcessStatus, Set<MessageProcessStatus>> LEGAL_TRANSITIONS;

    static {
        EnumMap<MessageProcessStatus, Set<MessageProcessStatus>> m = new EnumMap<>(MessageProcessStatus.class);
        m.put(MessageProcessStatus.RECEIVED,
                EnumSet.of(MessageProcessStatus.VALIDATED, MessageProcessStatus.FAILED));
        m.put(MessageProcessStatus.VALIDATED,
                EnumSet.of(MessageProcessStatus.PROCESSING, MessageProcessStatus.FAILED));
        m.put(MessageProcessStatus.PROCESSING,
                EnumSet.of(MessageProcessStatus.COMPLETED, MessageProcessStatus.FAILED));
        m.put(MessageProcessStatus.COMPLETED, EnumSet.noneOf(MessageProcessStatus.class));
        m.put(MessageProcessStatus.FAILED, EnumSet.noneOf(MessageProcessStatus.class));
        LEGAL_TRANSITIONS = Collections.unmodifiableMap(m);
    }

    private final MessageProcessStore store;

    /**
     * 注入持久化端口。生产环境由 Spring 注入 JPA 适配器，测试环境注入
     * {@link InMemoryMessageProcessStore}。
     *
     * @param store 报文处理记录存储，非空
     */
    public MessageStateMachine(final MessageProcessStore store) {
        this.store = store;
    }

    /**
     * 将指定记录的状态迁移至 {@code newStatus}。目标状态为
     * {@link MessageProcessStatus#FAILED} 时请改用
     * {@link #failWith(String, FepErrorCode, String)} 以携带错误信息。
     *
     * @param recordId  目标记录主键（32 位 UUID），非空
     * @param newStatus 目标状态，非空
     * @return 更新后的记录
     * @throws NoSuchElementException        指定 {@code recordId} 不存在
     * @throws IllegalMessageStateException  从当前状态到 {@code newStatus} 的转移非法
     */
    public MessageProcessRecord transition(final String recordId, final MessageProcessStatus newStatus) {
        MessageProcessRecord current = store.findById(recordId)
                .orElseThrow(() -> new NoSuchElementException("record not found: " + recordId));
        return transition(current, newStatus);
    }

    /**
     * 将已知记录迁移至 {@code newStatus}，跳过 {@code findById} 查询。
     * 调用方必须保证 {@code current} 是该记录的最新状态快照。
     *
     * @param current   当前记录快照，非空
     * @param newStatus 目标状态，非空
     * @return 更新后的记录
     * @throws IllegalMessageStateException 从当前状态到 {@code newStatus} 的转移非法
     */
    public MessageProcessRecord transition(final MessageProcessRecord current,
                                           final MessageProcessStatus newStatus) {
        assertLegal(current, newStatus);
        return store.updateStatus(current.getId(), newStatus, null, null);
    }

    /**
     * 便捷方法：将指定记录迁移至 {@link MessageProcessStatus#FAILED} 并写入错误元数据。
     * 要求当前状态非终态，否则抛出 {@link IllegalMessageStateException}。
     *
     * @param recordId     目标记录主键，非空
     * @param errorCode    错误码，非空
     * @param errorMessage 已脱敏的错误描述
     * @return 更新后的 FAILED 记录
     * @throws NoSuchElementException        指定 {@code recordId} 不存在
     * @throws IllegalMessageStateException  当前状态已为终态（COMPLETED / FAILED）
     */
    public MessageProcessRecord failWith(final String recordId,
                                         final FepErrorCode errorCode,
                                         final String errorMessage) {
        MessageProcessRecord current = store.findById(recordId)
                .orElseThrow(() -> new NoSuchElementException("record not found: " + recordId));
        return failWith(current, errorCode, errorMessage);
    }

    /**
     * 将已知记录迁移至 {@link MessageProcessStatus#FAILED}，跳过 {@code findById} 查询。
     *
     * @param current      当前记录快照，非空
     * @param errorCode    错误码，非空
     * @param errorMessage 已脱敏的错误描述
     * @return 更新后的 FAILED 记录
     * @throws IllegalMessageStateException 当前状态已为终态
     */
    public MessageProcessRecord failWith(final MessageProcessRecord current,
                                         final FepErrorCode errorCode,
                                         final String errorMessage) {
        assertLegal(current, MessageProcessStatus.FAILED);
        return store.updateStatus(current.getId(), MessageProcessStatus.FAILED,
                errorCode.getCode(), errorMessage);
    }

    private void assertLegal(final MessageProcessRecord current, final MessageProcessStatus target) {
        Set<MessageProcessStatus> allowed = LEGAL_TRANSITIONS.get(current.getStatus());
        if (!allowed.contains(target)) {
            throw new IllegalMessageStateException(String.format(
                    "Illegal transition: %s \u2192 %s (record id=%s, transitionNo=%s)",
                    current.getStatus(), target, current.getId(),
                    LogSanitizer.sanitize(current.getTransitionNo())));
        }
    }
}

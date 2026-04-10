package com.puchain.fep.processor.state;

import com.puchain.fep.converter.type.MessageType;

import java.time.Instant;
import java.util.Objects;

/**
 * 报文处理流转记录。不可变 POJO，所有状态变更通过 {@link #withStatus} 或 {@link #withFailure}
 * 返回新实例，原实例保持不变，可安全地跨线程共享。
 *
 * <p>采用传统 class + final fields（而非 Java record），以规避 SpotBugs
 * {@code EI_EXPOSE_REP} 对 record 组件的误报，同时在构造器中统一校验
 * 必填字段与 {@code transitionNo} 长度上限。</p>
 */
public final class MessageProcessRecord {

    /** {@code transitionNo} 最大长度，超出则构造失败。 */
    private static final int MAX_TRANSITION_NO_LENGTH = 30;

    private final String id;
    private final MessageType messageType;
    private final String transitionNo;
    private final MessageProcessStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final String errorCode;
    private final String errorMessage;

    /**
     * 全字段构造器。对必填字段执行非空校验，并验证 {@code transitionNo} 长度。
     *
     * @param id 32 位 UUID（由 {@code IdGenerator.uuid32()} 生成），非空
     * @param messageType 报文类型枚举，非空
     * @param transitionNo 流水号，非空且长度 ≤ {@value #MAX_TRANSITION_NO_LENGTH}
     * @param status 处理状态，非空
     * @param createdAt 创建时间，非空
     * @param updatedAt 最后更新时间，非空
     * @param errorCode 错误码，失败态必填，其他态可为 {@code null}
     * @param errorMessage 错误描述，失败态必填，其他态可为 {@code null}
     * @throws NullPointerException 任一必填字段为 {@code null}
     * @throws IllegalArgumentException {@code transitionNo} 长度超过上限
     */
    public MessageProcessRecord(final String id,
                                final MessageType messageType,
                                final String transitionNo,
                                final MessageProcessStatus status,
                                final Instant createdAt,
                                final Instant updatedAt,
                                final String errorCode,
                                final String errorMessage) {
        this.id = Objects.requireNonNull(id, "id");
        this.messageType = Objects.requireNonNull(messageType, "messageType");
        this.transitionNo = Objects.requireNonNull(transitionNo, "transitionNo");
        if (transitionNo.length() > MAX_TRANSITION_NO_LENGTH) {
            throw new IllegalArgumentException(
                    "transitionNo length > " + MAX_TRANSITION_NO_LENGTH + ": " + transitionNo.length());
        }
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * 创建初始 {@link MessageProcessStatus#RECEIVED} 记录，{@code createdAt}
     * 与 {@code updatedAt} 相同，错误信息为空。
     *
     * @param id 32 位 UUID
     * @param type 报文类型
     * @param transitionNo 流水号
     * @param now 当前时间戳
     * @return 新建的 RECEIVED 记录
     * @throws NullPointerException 任一参数为 {@code null}
     * @throws IllegalArgumentException {@code transitionNo} 超长
     */
    public static MessageProcessRecord initial(final String id,
                                               final MessageType type,
                                               final String transitionNo,
                                               final Instant now) {
        return new MessageProcessRecord(id, type, transitionNo,
                MessageProcessStatus.RECEIVED, now, now, null, null);
    }

    /**
     * 返回状态已更新的新实例，保留 {@code id} / {@code messageType} /
     * {@code transitionNo} / {@code createdAt} 与原错误信息，{@code updatedAt}
     * 替换为 {@code now}。
     *
     * <p>注意：本方法不校验状态转移合法性，合法性由
     * {@code MessageStateMachine}（Task 5）负责。</p>
     *
     * @param newStatus 目标状态，非空
     * @param now 更新时间戳，非空
     * @return 状态已更新的新实例
     * @throws NullPointerException 任一参数为 {@code null}
     */
    public MessageProcessRecord withStatus(final MessageProcessStatus newStatus, final Instant now) {
        return new MessageProcessRecord(id, messageType, transitionNo, newStatus,
                createdAt, now, errorCode, errorMessage);
    }

    /**
     * 返回状态为 {@link MessageProcessStatus#FAILED} 并携带错误信息的新实例。
     *
     * @param errorCode 错误码，建议来自 {@code FepErrorCode}
     * @param errorMessage 错误描述，必须已脱敏
     * @param now 更新时间戳，非空
     * @return FAILED 状态的新实例
     * @throws NullPointerException {@code now} 为 {@code null}
     */
    public MessageProcessRecord withFailure(final String errorCode,
                                            final String errorMessage,
                                            final Instant now) {
        return new MessageProcessRecord(id, messageType, transitionNo,
                MessageProcessStatus.FAILED, createdAt, now, errorCode, errorMessage);
    }

    public String getId() {
        return id;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getTransitionNo() {
        return transitionNo;
    }

    public MessageProcessStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}

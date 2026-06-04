package com.puchain.fep.web.requeststate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 请求生命周期聚合实体（S2 request-state tracking）。映射 {@code t_request_state}（V33）。
 *
 * <p>追踪一条 outbound 请求的 CREATED→SENT→RESULT_RECEIVED 生命周期（旁支 FAILED/STUCK），
 * correlation key = 8 位业务 transitionNo（两侧唯一共有，唯一约束）。镜像
 * {@code CallbackQueueEntity} 的「transition method only」设计：状态变更仅经 {@code markXxx}
 * 方法，外部不可直接写私有字段。时戳手动维护（不依赖 JPA Auditing）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "t_request_state",
        uniqueConstraints = @UniqueConstraint(name = "uk_request_state_correlation",
                columnNames = "correlation_key"))
public class RequestStateEntity {

    @Id
    @Column(name = "request_state_id", length = 32)
    private String requestStateId;

    @Column(name = "correlation_key", nullable = false, length = 32)
    private String correlationKey;

    @Column(name = "message_type", nullable = false, length = 8)
    private String messageType;

    @Column(name = "outbound_queue_id", length = 32)
    private String outboundQueueId;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 16)
    private RequestStateLifecycle lifecycleStatus;

    @Column(name = "correlation_blocked", nullable = false)
    private boolean correlationBlocked;

    @Column(name = "inbound_serial_no", length = 64)
    private String inboundSerialNo;

    @Column(name = "inbound_transition_no", length = 8)
    private String inboundTransitionNo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "result_received_at")
    private Instant resultReceivedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * JPA required no-arg constructor.
     */
    protected RequestStateEntity() {
    }

    private RequestStateEntity(final String correlationKey, final String messageType,
                              final String outboundQueueId, final boolean correlationBlocked) {
        this.requestStateId = UUID.randomUUID().toString().replace("-", "");
        this.correlationKey = Objects.requireNonNull(correlationKey, "correlationKey");
        this.messageType = Objects.requireNonNull(messageType, "messageType");
        this.outboundQueueId = outboundQueueId;
        this.correlationBlocked = correlationBlocked;
        this.lifecycleStatus = RequestStateLifecycle.CREATED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * 工厂：新建 {@link RequestStateLifecycle#CREATED} 行（outbound enqueue hook 调用）。
     *
     * @param correlationKey      8 位业务 transitionNo（归一后），非空，唯一
     * @param messageType         报文号（4 位，如 "3115"），非空
     * @param outboundQueueId     关联 {@code OutboundMessageQueueEntity.queueId}，可空
     * @param correlationBlocked  是否结构性永等不到匹配（见 {@link BlockedMessageTypes}）
     * @return CREATED 实体
     */
    public static RequestStateEntity created(final String correlationKey, final String messageType,
                                             final String outboundQueueId,
                                             final boolean correlationBlocked) {
        return new RequestStateEntity(correlationKey, messageType, outboundQueueId, correlationBlocked);
    }

    /**
     * 标记已发送：{@link RequestStateLifecycle#CREATED} → {@link RequestStateLifecycle#SENT}，
     * 记 {@code sentAt}（outbound TLQ send 成功 hook）。
     */
    public void markSent() {
        this.lifecycleStatus = RequestStateLifecycle.SENT;
        this.sentAt = Instant.now();
        this.updatedAt = this.sentAt;
    }

    /**
     * 标记结果已返回：→ {@link RequestStateLifecycle#RESULT_RECEIVED}（happy 终态），回填
     * inbound 字段并记 {@code resultReceivedAt}（inbound 结果归一 transitionNo 匹配 hook）。
     *
     * @param serialNo            inbound 结果业务流水号，可空
     * @param inboundTransitionNo inbound 归一 8 位 transitionNo，可空
     */
    public void markResultReceived(final String serialNo, final String inboundTransitionNo) {
        this.lifecycleStatus = RequestStateLifecycle.RESULT_RECEIVED;
        this.inboundSerialNo = serialNo;
        this.inboundTransitionNo = inboundTransitionNo;
        this.resultReceivedAt = Instant.now();
        this.updatedAt = this.resultReceivedAt;
    }

    /**
     * 标记失败：→ {@link RequestStateLifecycle#FAILED}（outbound 永久失败 / DLQ）。
     */
    public void markFailed() {
        this.lifecycleStatus = RequestStateLifecycle.FAILED;
        this.updatedAt = Instant.now();
    }

    /**
     * 标记滞留：→ {@link RequestStateLifecycle#STUCK}（reaper 对 SENT 超 TTL 无结果的非
     * correlation_blocked 行调用）。
     */
    public void markStuck() {
        this.lifecycleStatus = RequestStateLifecycle.STUCK;
        this.updatedAt = Instant.now();
    }

    /**
     * @return 主键（32 位无连字符 UUID）
     */
    public String getRequestStateId() {
        return requestStateId;
    }

    /**
     * @return correlation key（8 位业务 transitionNo，唯一）
     */
    public String getCorrelationKey() {
        return correlationKey;
    }

    /**
     * @return 报文号（4 位）
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * @return 关联 outbound 队列主键（可空）
     */
    public String getOutboundQueueId() {
        return outboundQueueId;
    }

    /**
     * @return 当前生命周期状态
     */
    public RequestStateLifecycle getLifecycleStatus() {
        return lifecycleStatus;
    }

    /**
     * @return 是否结构性永等不到匹配（reaper STUCK 检测排除标志）
     */
    public boolean isCorrelationBlocked() {
        return correlationBlocked;
    }

    /**
     * @return inbound 结果业务流水号（未返回时为 null）
     */
    public String getInboundSerialNo() {
        return inboundSerialNo;
    }

    /**
     * @return inbound 归一 8 位 transitionNo（未返回时为 null）
     */
    public String getInboundTransitionNo() {
        return inboundTransitionNo;
    }

    /**
     * @return 创建时间（不可变）
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * @return 发送时间（markSent 后非空）
     */
    public Instant getSentAt() {
        return sentAt;
    }

    /**
     * @return 结果返回时间（markResultReceived 后非空）
     */
    public Instant getResultReceivedAt() {
        return resultReceivedAt;
    }

    /**
     * @return 最近状态变更时间
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

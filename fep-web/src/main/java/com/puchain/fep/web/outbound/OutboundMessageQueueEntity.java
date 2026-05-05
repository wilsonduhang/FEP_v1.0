package com.puchain.fep.web.outbound;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity for the {@code outbound_message_queue} table — base schema by Flyway
 * {@code V22__create_outbound_message_queue.sql} (P4 T7a, 14 columns) +
 * 3 observability columns added by {@code V25__outbound_queue_observability_columns.sql}
 * (P5 T1: {@code sent_at}, {@code msg_id}, {@code tlq_send_result}).
 *
 * <p>Persisted by {@link JpaOutboundMessageEnqueueService} on behalf of
 * fep-collector via the {@code OutboundMessageEnqueuePort} contract; consumed
 * by the P5 TLQ outbound dispatcher (Consumer/Runner — see Plan §Task 2/T6/T9).</p>
 *
 * <p>Status string instead of enum is intentional for V1 — downstream consumer
 * (T8/T9) is the single writer that mutates {@code status}, so a typed enum is
 * deferred to that Task. {@code idempotency_key} is the natural-key uniqueness
 * guard (DB-level UNIQUE constraint {@code uk_outbound_queue_idempotency_key}).</p>
 *
 * <p><b>{@code status} 字段 6 值定义</b>（ADR
 * {@code docs/decisions/2026-05-04-outbound-status-machine.md} §5 仲裁方案 B）:</p>
 * <ul>
 *   <li>{@code PENDING} — 入队初始态（P4 collector 写入）</li>
 *   <li>{@code PROCESSING} — P5 consumer 持锁中</li>
 *   <li>{@code SENT} — TLQ 发送成功（terminal-success）</li>
 *   <li>{@code FAILED} — 单次失败瞬时态（短暂，立即转 {@code RETRY} 或 {@code DEAD_LETTER}）</li>
 *   <li>{@code RETRY} — exp_backoff 退避中，{@code next_retry_at} 调度</li>
 *   <li>{@code DEAD_LETTER} — {@code retry_count >= 5} 终止重试（terminal-DLQ）</li>
 * </ul>
 *
 * <p><b>注:</b> V22 SQL line 19 注释仅列 4 值（PENDING/PROCESSING/SENT/FAILED）是 comment
 * 不完整，以 V25 + 本 Javadoc 为准。V22 文件本身不动（Flyway F 级硬冻结）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "outbound_message_queue")
public class OutboundMessageQueueEntity {

    @Id
    @Column(name = "queue_id", nullable = false, length = 32)
    private String queueId;

    @Column(name = "message_type", nullable = false, length = 8)
    private String messageType;

    @Column(name = "transition_no", nullable = false, length = 30)
    private String transitionNo;

    @Column(name = "idempotency_key", nullable = false, length = 64, unique = true)
    private String idempotencyKey;

    @Column(name = "message_head_xml", nullable = false, columnDefinition = "TEXT")
    private String messageHeadXml;

    @Column(name = "message_body_xml", nullable = false, columnDefinition = "TEXT")
    private String messageBodyXml;

    @Column(name = "payload_data_type", nullable = false, length = 64)
    private String payloadDataType;

    @Column(name = "source_ref", length = 255)
    private String sourceRef;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** TLQ 发送成功时间（V25 加列），NULL 表示未发送或失败。 */
    @Column(name = "sent_at")
    private Instant sentAt;

    /** TLQ 业务 MsgId（PRD §3.1.3 14 datetime + 6 seq numeric，VARCHAR(20)），SENT 终态写入。 */
    @Column(name = "msg_id", length = 20)
    private String msgId;

    /** TLQ SendResult.toString() 截断 64 字符，用于回放排查（成功 "ok:..." / 失败 "fail:..."）。 */
    @Column(name = "tlq_send_result", length = 64)
    private String tlqSendResult;

    /**
     * No-arg constructor required by JPA.
     */
    public OutboundMessageQueueEntity() {
        // JPA
    }

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(final String queueId) {
        this.queueId = queueId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(final String messageType) {
        this.messageType = messageType;
    }

    public String getTransitionNo() {
        return transitionNo;
    }

    public void setTransitionNo(final String transitionNo) {
        this.transitionNo = transitionNo;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(final String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getMessageHeadXml() {
        return messageHeadXml;
    }

    public void setMessageHeadXml(final String messageHeadXml) {
        this.messageHeadXml = messageHeadXml;
    }

    public String getMessageBodyXml() {
        return messageBodyXml;
    }

    public void setMessageBodyXml(final String messageBodyXml) {
        this.messageBodyXml = messageBodyXml;
    }

    public String getPayloadDataType() {
        return payloadDataType;
    }

    public void setPayloadDataType(final String payloadDataType) {
        this.payloadDataType = payloadDataType;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(final String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(final int retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(final Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(final Instant sentAt) {
        this.sentAt = sentAt;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(final String msgId) {
        this.msgId = msgId;
    }

    public String getTlqSendResult() {
        return tlqSendResult;
    }

    public void setTlqSendResult(final String tlqSendResult) {
        this.tlqSendResult = tlqSendResult;
    }
}

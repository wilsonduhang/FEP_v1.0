package com.puchain.fep.web.outbound;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity for the {@code outbound_message_queue} table created by Flyway
 * migration {@code V22__create_outbound_message_queue.sql} (P4 T7a).
 *
 * <p>Persisted by {@link JpaOutboundMessageEnqueueService} on behalf of
 * fep-collector via the {@code OutboundMessageEnqueuePort} contract; consumed
 * by P5+ TLQ outbound dispatcher (not in this Task's scope).</p>
 *
 * <p>Status string instead of enum is intentional for V1 — downstream consumer
 * (T8/T9) is the single writer that mutates {@code status}, so a typed enum is
 * deferred to that Task. {@code idempotency_key} is the natural-key uniqueness
 * guard (DB-level UNIQUE constraint {@code uk_outbound_queue_idempotency_key}).</p>
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
}

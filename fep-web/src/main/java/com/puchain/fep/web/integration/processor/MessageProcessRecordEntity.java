package com.puchain.fep.web.integration.processor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity mapping for the {@code message_process_record} table created by
 * Flyway migration {@code V16__create_p2a_message_process_record.sql}.
 *
 * <p>Columns are mapped one-to-one against V16 with the following conventions:</p>
 * <ul>
 *   <li>{@code id} — 32-char UUID primary key.</li>
 *   <li>{@code messageType} — 4-digit HNDEMP message code (e.g. {@code 1001}),
 *       converted to/from {@link com.puchain.fep.converter.type.MessageType} by the adapter.</li>
 *   <li>{@code status} — {@link com.puchain.fep.processor.state.MessageProcessStatus} name,
 *       stored as string for forward-compat and auditability.</li>
 *   <li>{@code createdAt}/{@code updatedAt} — epoch millis; {@link java.time.Instant}
 *       conversion lives in the adapter so the entity has no temporal dependency.</li>
 * </ul>
 *
 * <p>This class is a plain mutable POJO (JPA requirement); domain invariants are
 * enforced by {@link com.puchain.fep.processor.state.MessageProcessRecord}.</p>
 */
@Entity
@Table(name = "message_process_record")
public class MessageProcessRecordEntity {

    @Id
    @Column(name = "id", length = 32, nullable = false)
    private String id;

    @Column(name = "message_type", length = 8, nullable = false)
    private String messageType;

    @Column(name = "transition_no", length = 30, nullable = false, unique = true)
    private String transitionNo;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Column(name = "error_code", length = 16)
    private String errorCode;

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    /**
     * No-arg constructor required by JPA.
     */
    public MessageProcessRecordEntity() {
        // JPA
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(final long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(final String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

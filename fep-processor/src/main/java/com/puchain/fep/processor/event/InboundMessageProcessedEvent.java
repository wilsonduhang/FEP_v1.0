package com.puchain.fep.processor.event;

import com.puchain.fep.converter.type.MessageType;

import java.time.Instant;
import java.util.Objects;

/**
 * Application event published after an inbound HNDEMP message has been
 * decoded, validated, and persisted by the synchronous processor pipeline.
 *
 * <p>P3 Task 1 — message-driven wiring foundation. The pipeline emits this
 * event so downstream listeners (reconciliation services, audit, metrics, ...)
 * can react asynchronously without the processor having a hard dependency
 * on any single consumer. ArchUnit guards still forbid the processor layer
 * from depending on {@code fep-web}; this record stays in the converter +
 * java.* surface so the rule keeps holding.</p>
 *
 * <p>Field semantics:</p>
 * <ul>
 *   <li>{@code type}        — non-null HNDEMP message type (e.g. {@code MSG_3107}).</li>
 *   <li>{@code transitionNo}— non-null inbound transition number (8 digits, last
 *                              segment of the original {@code RealHead.transitionNo}).</li>
 *   <li>{@code serialNo}    — non-null business serial number used to pair
 *                              with the outbound request that triggered this inbound.</li>
 *   <li>{@code body}        — nullable decoded business body POJO; listeners must
 *                              null-check before they cast / dispatch on its type.</li>
 *   <li>{@code occurredAt}  — non-null event creation timestamp (UTC).</li>
 * </ul>
 *
 * <p>The compact constructor enforces non-null on every field except
 * {@code body}; {@code body} can legitimately be {@code null} when the
 * pipeline persists a record but skipped body unmarshalling (e.g. on a
 * validation error path); listeners must defend themselves accordingly.</p>
 *
 * @param type         non-null message type
 * @param transitionNo non-null transition number
 * @param serialNo     non-null business serial number
 * @param body         nullable decoded body
 * @param occurredAt   non-null event creation time
 * @author FEP Team
 * @since 1.0.0
 */
public record InboundMessageProcessedEvent(
        MessageType type,
        String transitionNo,
        String serialNo,
        Object body,
        Instant occurredAt) {

    /**
     * Compact constructor enforcing non-null invariants on every field
     * except {@code body} (allowed to be {@code null} by design).
     */
    public InboundMessageProcessedEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(transitionNo, "transitionNo");
        Objects.requireNonNull(serialNo, "serialNo");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}

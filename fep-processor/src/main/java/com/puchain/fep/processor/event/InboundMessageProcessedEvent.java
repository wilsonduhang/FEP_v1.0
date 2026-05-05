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

    /**
     * Casts {@link #body()} to the {@code expected} type, returning {@code null}
     * when the body itself is {@code null} (consistent with listener {@code raw
     * == null → silent skip} semantics) and throwing {@link IllegalStateException}
     * when the body is non-null but is not assignable to {@code expected}.
     *
     * <p>Replaces the duplicated {@code instanceof X body → throw ISE} pattern
     * in {@code BankReconciliationEventListener} / {@code ClearingInstructionEventListener}
     * / {@code PlatformReconciliationEventListener} (P3-DEFER-LISTENER-BODYCAST,
     * a.k.a. R2). The thrown ISE keeps the registry-contract-violation contract
     * documented on each listener's class Javadoc — when the dispatcher rolls
     * back its {@code @Transactional} boundary, {@code message_process_record}
     * and the per-listener side tables stay consistent.</p>
     *
     * <p>Sub-type matching follows {@link Class#isInstance(Object)} semantics
     * (identical to {@code instanceof}); a {@code java.util.ArrayList} body
     * passed through {@code bodyAs(java.util.List.class)} returns the same
     * reference, no copy.</p>
     *
     * @param <T>      the expected body type
     * @param expected the {@link Class} guard, non-null
     * @return the typed body, or {@code null} when {@link #body()} is {@code null}
     * @throws NullPointerException  when {@code expected} itself is {@code null}
     * @throws IllegalStateException when the body is non-null but not assignable
     *                               to {@code expected}
     */
    public <T> T bodyAs(final Class<T> expected) {
        Objects.requireNonNull(expected, "expected");
        final Object current = body();
        if (current == null) {
            return null;
        }
        if (!expected.isInstance(current)) {
            throw new IllegalStateException(
                    "event.body type mismatch: expected "
                            + expected.getSimpleName()
                            + ", got "
                            + current.getClass().getName());
        }
        return expected.cast(current);
    }
}

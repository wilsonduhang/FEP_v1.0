package com.puchain.fep.processor.event;

import com.puchain.fep.converter.type.MessageType;

import java.time.Instant;
import java.util.Objects;

/**
 * Application event published after the batch (non-real-time) pipeline finishes
 * processing a {@code CfxMessage} (§6.4.1 FR-DATA-DB-01, PRD v1.3 §2020 非实时业务转发记录表).
 *
 * <p>Mirrors {@link InboundMessageProcessedEvent}: the {@code fep-processor}
 * pipeline emits this event so a downstream {@code fep-web} listener can persist
 * a {@code batch_forward_records} row without the processor depending on the web
 * layer. ArchUnit guards still forbid the processor layer from depending on
 * {@code fep-web}; this record stays on the converter + {@code java.*} surface so
 * the rule keeps holding.</p>
 *
 * <p>Field semantics:</p>
 * <ul>
 *   <li>{@code type}        — non-null batch message type (e.g. {@code MSG_3009}).</li>
 *   <li>{@code transitionNo}— non-null batch transition number (the source serial:
 *                              {@code head.msgId} when present, else a generated id;
 *                              used as the idempotency key on the side table).</li>
 *   <li>{@code total}       — total record count in the batch (&ge; 0).</li>
 *   <li>{@code success}     — successfully validated record count (&ge; 0).</li>
 *   <li>{@code failed}      — failed record count ({@code total - success}, &ge; 0).</li>
 *   <li>{@code startedAt}   — non-null batch processing start timestamp (UTC).</li>
 *   <li>{@code finishedAt}  — non-null batch processing finish timestamp (UTC).</li>
 * </ul>
 *
 * @param type         non-null batch message type
 * @param transitionNo non-null batch transition number
 * @param total        total record count
 * @param success      successful record count
 * @param failed       failed record count
 * @param startedAt    non-null start timestamp
 * @param finishedAt   non-null finish timestamp
 * @author FEP Team
 * @since 1.0.0
 */
public record BatchForwardProcessedEvent(
        MessageType type,
        String transitionNo,
        int total,
        int success,
        int failed,
        Instant startedAt,
        Instant finishedAt) {

    /**
     * Compact constructor enforcing non-null invariants on the reference fields.
     */
    public BatchForwardProcessedEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(transitionNo, "transitionNo");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(finishedAt, "finishedAt");
    }
}

package com.puchain.fep.processor.event;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link InboundMessageProcessedEvent}.
 *
 * <p>P3 Task 1 — locks down the contract of the inbound event payload:</p>
 * <ul>
 *   <li>Compact constructor rejects null on every required field.</li>
 *   <li>Java record value semantics (equals / hashCode) work as expected.</li>
 *   <li>{@code toString()} contract is exercised so we are aware of what
 *       the default record formatter exposes (every field name + value);
 *       any future migration to a sanitising override has a sentinel here.</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class InboundMessageProcessedEventTest {

    private static final MessageType TYPE = MessageType.MSG_3108;
    private static final String TRANSITION_NO = "00000001";
    private static final String SERIAL_NO = "RC_20260428_001";
    private static final Object BODY = new Object();
    private static final Instant OCCURRED_AT = Instant.parse("2026-04-28T10:00:00Z");

    @Test
    void compactConstructor_rejectsNullOnRequiredFields() {
        // type
        assertThatThrownBy(() -> new InboundMessageProcessedEvent(
                null, TRANSITION_NO, SERIAL_NO, BODY, OCCURRED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("type");

        // transitionNo
        assertThatThrownBy(() -> new InboundMessageProcessedEvent(
                TYPE, null, SERIAL_NO, BODY, OCCURRED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("transitionNo");

        // serialNo
        assertThatThrownBy(() -> new InboundMessageProcessedEvent(
                TYPE, TRANSITION_NO, null, BODY, OCCURRED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("serialNo");

        // occurredAt
        assertThatThrownBy(() -> new InboundMessageProcessedEvent(
                TYPE, TRANSITION_NO, SERIAL_NO, BODY, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("occurredAt");

        // body is allowed to be null by design
        InboundMessageProcessedEvent event = new InboundMessageProcessedEvent(
                TYPE, TRANSITION_NO, SERIAL_NO, null, OCCURRED_AT);
        assertThat(event.body()).isNull();
    }

    @Test
    void equals_returnsTrueForRecordsWithIdenticalFields() {
        Object sharedBody = new Object();
        InboundMessageProcessedEvent left = new InboundMessageProcessedEvent(
                TYPE, TRANSITION_NO, SERIAL_NO, sharedBody, OCCURRED_AT);
        InboundMessageProcessedEvent right = new InboundMessageProcessedEvent(
                TYPE, TRANSITION_NO, SERIAL_NO, sharedBody, OCCURRED_AT);

        assertThat(left).isEqualTo(right);

        InboundMessageProcessedEvent different = new InboundMessageProcessedEvent(
                MessageType.MSG_3107, TRANSITION_NO, SERIAL_NO, sharedBody, OCCURRED_AT);
        assertThat(left).isNotEqualTo(different);
    }

    @Test
    void hashCode_isStableForRecordsWithIdenticalFields() {
        Object sharedBody = new Object();
        InboundMessageProcessedEvent left = new InboundMessageProcessedEvent(
                TYPE, TRANSITION_NO, SERIAL_NO, sharedBody, OCCURRED_AT);
        InboundMessageProcessedEvent right = new InboundMessageProcessedEvent(
                TYPE, TRANSITION_NO, SERIAL_NO, sharedBody, OCCURRED_AT);

        assertThat(left.hashCode()).isEqualTo(right.hashCode());
    }

    @Test
    void toString_containsAllFieldNames() {
        // Sentinel guarding the default Java record toString contract.
        // If a future PR overrides toString() (e.g. to mask serialNo) this
        // assertion makes the change visible. Today, the default formatter
        // emits every field name + value.
        InboundMessageProcessedEvent event = new InboundMessageProcessedEvent(
                TYPE, TRANSITION_NO, SERIAL_NO, BODY, OCCURRED_AT);

        String text = event.toString();
        assertThat(text)
                .contains("type=")
                .contains("transitionNo=")
                .contains("serialNo=")
                .contains("body=")
                .contains("occurredAt=");
    }
}

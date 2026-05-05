package com.puchain.fep.processor.event;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    // ===== R2 (P3-DEFER-LISTENER-BODYCAST) — bodyAs helper tests (hot-fix v0.5.1 append) =====
    //
    // Closes P3-DEFER-LISTENER-BODYCAST. Replaces the duplicated `raw == null
    // → return; instanceof X body → throw ISE` pattern in 3 listeners with a
    // single record-level helper. 5 new tests below cover null body / typed
    // match / type mismatch ISE / NPE on null Class<T> / subtype match.
    private static final Instant BODYAS_FIXED_TS =
            Instant.parse("2026-05-05T10:00:00Z");

    private static InboundMessageProcessedEvent eventWithBody(final Object body) {
        return new InboundMessageProcessedEvent(
                MessageType.MSG_3116,
                "00000001",
                "SERIAL-001",
                body,
                BODYAS_FIXED_TS);
    }

    @Test
    void bodyAs_shouldReturnNull_whenBodyIsNull() {
        final InboundMessageProcessedEvent event = eventWithBody(null);

        final String result = event.bodyAs(String.class);

        assertThat(result).isNull();
    }

    @Test
    void bodyAs_shouldReturnTyped_whenBodyMatchesExpected() {
        final String body = "payload-3116";
        final InboundMessageProcessedEvent event = eventWithBody(body);

        final String result = event.bodyAs(String.class);

        assertThat(result).isSameAs(body);
    }

    @Test
    void bodyAs_shouldThrowISE_whenBodyTypeMismatch() {
        final Integer body = 42;
        final InboundMessageProcessedEvent event = eventWithBody(body);

        assertThatThrownBy(() -> event.bodyAs(String.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("event.body type mismatch")
                .hasMessageContaining("expected String")
                .hasMessageContaining("got java.lang.Integer");
    }

    @Test
    void bodyAs_shouldThrowNPE_whenExpectedClassIsNull() {
        final InboundMessageProcessedEvent event = eventWithBody("any");

        assertThatThrownBy(() -> event.bodyAs(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("expected");
    }

    @Test
    void bodyAs_shouldReturnTyped_whenBodyIsSubtypeOfExpected() {
        // 验证 Class.cast 不深拷贝（隐性 API 契约）：ArrayList → List 上溯返回同引用
        final ArrayList<String> body = new ArrayList<>();
        final InboundMessageProcessedEvent event = eventWithBody(body);

        final List<?> result = event.bodyAs(List.class);

        assertThat(result).isSameAs(body);
    }
}

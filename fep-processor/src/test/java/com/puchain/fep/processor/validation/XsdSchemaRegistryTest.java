package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.xml.validation.Schema;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XsdSchemaRegistry} — central registry of compiled XSD
 * {@link Schema} instances keyed by {@link MessageType}.
 *
 * <p>Coverage:</p>
 * <ul>
 *     <li>{@code schemaOf} returns non-null for every registered {@link MessageType}</li>
 *     <li>{@code schemaOf} caches compiled {@link Schema} instances (identity equality)</li>
 *     <li>{@code schemaOf} is thread-safe under concurrent access</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class XsdSchemaRegistryTest {

    private static final List<MessageType> SUPPORTED = List.of(
            MessageType.MSG_1001, MessageType.MSG_1004,
            MessageType.MSG_1102, MessageType.MSG_1103, MessageType.MSG_1104,
            MessageType.MSG_2001, MessageType.MSG_2004,
            MessageType.MSG_2102, MessageType.MSG_2103, MessageType.MSG_2104,
            MessageType.MSG_3001, MessageType.MSG_3002,
            MessageType.MSG_3003, MessageType.MSG_3004,
            MessageType.MSG_3005, MessageType.MSG_3006,
            MessageType.MSG_3008, MessageType.MSG_3020,
            MessageType.MSG_3103, MessageType.MSG_3108,
            MessageType.MSG_9005, MessageType.MSG_9120
    );

    private static XsdSchemaRegistry registry;

    @BeforeAll
    static void init() {
        registry = new XsdSchemaRegistry();
    }

    @Test
    void schemaOf_shouldReturnNonNullForAllSupportedMessages() {
        SUPPORTED.forEach(type -> {
            Schema schema = registry.schemaOf(type);
            assertThat(schema).as("schema for %s", type.msgNo()).isNotNull();
        });
    }

    @Test
    void schemaOf_shouldReturnCachedInstance() {
        Schema first = registry.schemaOf(MessageType.MSG_1001);
        Schema second = registry.schemaOf(MessageType.MSG_1001);
        assertThat(second).isSameAs(first);
    }

    // schemaOf_shouldThrowForUnsupportedMessage (real-enum form) removed: P4-MSG-D
    // added MSG_1101 to XsdSchemaRegistry.SUPPORTED_CODES (and indeed all 44
    // MessageType enum values are now mapped). The defensive
    // UnsupportedOperationException branch in schemaOf() is therefore statically
    // unreachable through the real enum, but retained for future MessageType
    // additions that lag XSD load. Mockito-based coverage of that branch lives
    // in schemaOf_unknownMessageType_throwsUnsupportedOperationException below
    // (Q-P2-2 — E-NIT-1 closing Simplify deferred).

    @Test
    @DisplayName("schemaOf(unregistered MessageType) throws UnsupportedOperationException "
            + "— covers defensive branch (statically unreachable, retained for future growth)")
    void schemaOf_unknownMessageType_throwsUnsupportedOperationException() {
        // Reuse Task 2 (E-NIT-7) module-level singleton; mocks an out-of-scope
        // MessageType so cache.get() returns null and the defensive throw fires.
        MessageType mockType = mock(MessageType.class);
        when(mockType.msgNo()).thenReturn("9999"); // outside SUPPORTED_CODES

        assertThatThrownBy(() -> AbstractXsdValidationTest.SHARED_REGISTRY.schemaOf(mockType))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("MessageType 9999 is not supported")
                .hasMessageContaining("Supported:");
    }

    @Test
    void schemaOf_shouldBeThreadSafe() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(16);
        AtomicInteger failures = new AtomicInteger();
        for (int i = 0; i < 100; i++) {
            pool.submit(() -> {
                try {
                    for (MessageType type : SUPPORTED) {
                        if (registry.schemaOf(type) == null) {
                            failures.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                }
            });
        }
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        assertThat(failures.get()).isZero();
    }
}

package com.puchain.fep.processor.routing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RoleDirection} enum constraint (Plan R D1).
 *
 * <p>Verifies ADR-R-1 decision: 3 meaningful states only
 * (OUTBOUND_ACTIVE / INBOUND_PASSIVE / NOT_APPLICABLE); OUTBOUND_ACK /
 * INBOUND_ACK pruned as 0-ref reserved values.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class RoleDirectionTest {

    @Test
    void enumValues_shouldContainExactlyThreeMeaningfulStates() {
        assertThat(RoleDirection.values())
                .containsExactly(
                        RoleDirection.OUTBOUND_ACTIVE,
                        RoleDirection.INBOUND_PASSIVE,
                        RoleDirection.NOT_APPLICABLE);
    }
}

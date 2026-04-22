package com.puchain.fep.processor.routing;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

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

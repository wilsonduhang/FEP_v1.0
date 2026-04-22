package com.puchain.fep.processor.routing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProcessingMode} enum constraint (Plan R D1).
 *
 * <p>Verifies ADR-R-1 decision: 4 active modes only (MODE_1/2/3/5);
 * MODE_4 / MODE_6 pruned as 0-ref reserved values.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class ProcessingModeTest {

    @Test
    void enumValues_shouldContainFourActiveModes() {
        assertThat(ProcessingMode.values())
                .containsExactly(
                        ProcessingMode.MODE_1,
                        ProcessingMode.MODE_2,
                        ProcessingMode.MODE_3,
                        ProcessingMode.MODE_5);
    }
}

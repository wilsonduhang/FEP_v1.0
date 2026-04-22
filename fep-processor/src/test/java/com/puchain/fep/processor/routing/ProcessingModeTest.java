package com.puchain.fep.processor.routing;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

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

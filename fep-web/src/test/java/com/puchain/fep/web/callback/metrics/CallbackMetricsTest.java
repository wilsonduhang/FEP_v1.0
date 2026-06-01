package com.puchain.fep.web.callback.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CallbackMetricsTest {

    @Test
    void recordSent_shouldIncrementSentCounterAndTimer() {
        final SimpleMeterRegistry reg = new SimpleMeterRegistry();
        final CallbackMetrics metrics = new CallbackMetrics(reg);
        metrics.recordSent(1_000_000L);
        assertThat(reg.counter("fep_callback_send_total", "status", "SENT").count()).isEqualTo(1.0);
        assertThat(reg.timer("fep_callback_send_latency_seconds", "status", "SENT").count()).isEqualTo(1L);
    }

    @Test
    void recordRetry_andDeadLetter_shouldIncrementTaggedCounters() {
        final SimpleMeterRegistry reg = new SimpleMeterRegistry();
        final CallbackMetrics metrics = new CallbackMetrics(reg);
        metrics.recordRetry();
        metrics.recordDeadLetter();
        assertThat(reg.counter("fep_callback_send_total", "status", "RETRY").count()).isEqualTo(1.0);
        assertThat(reg.counter("fep_callback_send_total", "status", "DEAD_LETTER").count()).isEqualTo(1.0);
    }
}

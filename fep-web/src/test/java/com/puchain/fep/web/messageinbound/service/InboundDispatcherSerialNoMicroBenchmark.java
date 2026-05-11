package com.puchain.fep.web.messageinbound.service;

import com.puchain.fep.converter.model.SerialNoBearing;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E-3 closing micro-benchmark — 测 {@code extractSerialNo} 单次调用平均耗时
 * 给 FR-NFR-PERF 主追溯提供 measurable datapoint（v0.2 santa Round 1 Reviewer B 提出）。
 *
 * <p>v0.4 修订（santa Round 3 Reviewer B'' F7 + Reviewer C'' S2/S1）:
 * (a) <b>Batch timing</b>: 内层 BATCH=100 calls / 1 次 nanoTime delta，摊销 nanoTime
 *     仪器双 bracket 开销（macOS Darwin x86_64 nanoTime granularity ~30-100 ns，
 *     Apple Silicon ~41 ns，Linux x86 ~20-50 ns；摊销后每 call 仪器开销 &lt; 1 ns 不再主导测量）；
 * (b) <b>SINK volatile</b> 防 JIT dead-code elimination（防 instanceof + getter 整链被
 *     escape analysis 优化掉）；
 * (c) 阈值上调 P50 ≤ 100 ns / P95 ≤ 500 ns，吸纳 JaCoCo agent instrumentation overhead
 *     （生产 dispatcher.extractSerialNo body 3-4 probes × 10-50 ns/probe = 30-200 ns
 *     hot path tax）+ macOS APFS 抖动余量（红线 feedback_macos_apfs_fork_classloader_race）；
 * (d) <b>JaCoCo exclude</b>: jacoco-maven-plugin 配置
 *     {@code <exclude>**}/{@code InboundDispatcherSerialNoMicroBenchmark.class</exclude>}
 *     避开 benchmark class 自身 instrumented bytecode。
 *
 * <p>v0.3 baseline 修订：extractSerialNo 改 package-private（同包直接调，无
 * Method.invoke 反射 wrapper），输出改 SLF4J Logger.info（避免 Checkstyle ban
 * System.out/err）。
 *
 * <p>不上 JMH（per-msg 影响 ~ns 级，JAXB unmarshal 主导 dispatcher P95 ~ms 级）；
 * batch-timed nanoTime warmup-then-measure 模式即足够给 closing report 引用。
 */
class InboundDispatcherSerialNoMicroBenchmark {

    private static final Logger LOG =
            LoggerFactory.getLogger(InboundDispatcherSerialNoMicroBenchmark.class);

    private static final int WARMUP = 1000;
    private static final int MEASURE = 1000;
    private static final int BATCH = 100;
    private static final SerialNoBearing FIXTURE = () -> "SN_BENCH001";
    private static final String TRANSITION_NO = "TX_BENCH";
    private static volatile String sink;

    @Test
    void instanceofPath_p50AndP95_meetThresholds() {
        for (int i = 0; i < WARMUP; i++) {
            for (int j = 0; j < BATCH; j++) {
                sink = InboundMessageDispatcher.extractSerialNo(FIXTURE, TRANSITION_NO);
            }
        }
        long[] samples = new long[MEASURE];
        for (int i = 0; i < MEASURE; i++) {
            long t0 = System.nanoTime();
            for (int j = 0; j < BATCH; j++) {
                sink = InboundMessageDispatcher.extractSerialNo(FIXTURE, TRANSITION_NO);
            }
            samples[i] = (System.nanoTime() - t0) / BATCH;
        }
        Arrays.sort(samples);
        long p50 = samples[MEASURE / 2];
        long p95 = samples[(int) (MEASURE * 0.95)];
        double avg = Arrays.stream(samples).average().orElse(0);

        LOG.info("[E-3 perf] avg={} ns/call  p50={} ns  p95={} ns  "
                        + "(samples={}, batch={}, warmup={}x{}, sink_ref={})",
                String.format("%.1f", avg), p50, p95, MEASURE, BATCH, WARMUP, BATCH,
                sink == null ? "null" : "set");

        assertThat(p50).as("p50 of batched instanceof path (JaCoCo-tolerant)")
                .isLessThanOrEqualTo(100);
        assertThat(p95).as("p95 with macOS APFS + JaCoCo + GC jitter margin")
                .isLessThanOrEqualTo(500);
    }
}

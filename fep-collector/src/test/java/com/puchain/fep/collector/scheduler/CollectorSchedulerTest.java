package com.puchain.fep.collector.scheduler;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.collector.assembler.PayloadAssembler;
import com.puchain.fep.collector.run.CollectionRunRecorder;
import com.puchain.fep.collector.support.AdapterType;
import com.puchain.fep.collector.support.CollectionMetrics;
import com.puchain.fep.collector.support.CollectionMetricsSnapshot;
import com.puchain.fep.collector.support.CollectionRecord;
import com.puchain.fep.collector.support.CollectionRunContext;
import com.puchain.fep.collector.support.CollectionRunResult;
import com.puchain.fep.collector.support.CollectorAdapter;
import com.puchain.fep.collector.support.DistributedLock;
import com.puchain.fep.collector.support.IdempotencyKeyGenerator;
import com.puchain.fep.collector.support.LockToken;
import com.puchain.fep.collector.support.TriggerType;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.processor.intake.port.Direction;
import com.puchain.fep.processor.intake.port.EnqueueResult;
import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import com.puchain.fep.processor.intake.port.OutboundMessageEnqueuePort;
import com.puchain.fep.processor.intake.port.OutboundMessageEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CollectorScheduler} 单元测试（Mockito，无 Spring context）。
 *
 * <p>覆盖 Plan §T6 验收 #2/#3/#4/#5/#8（10 个测试，含 T6a-fix 4 个）：
 * <ul>
 *   <li>{@code success} — 干净路径，3 条记录全成功 → SUCCESS</li>
 *   <li>{@code skipped(lock)} — tryLock empty → SKIPPED 短路</li>
 *   <li>{@code partial} — assemble 异常 + dup-key + 成功各一 → PARTIAL，errors=1，submitted=2</li>
 *   <li>{@code duplicate} — 全 dup-key → SUCCESS（不算 error），submitted=N</li>
 *   <li>{@code trigger-rejected(disabled)} — adapter enabled=false → COLLECT_TRIGGER_REJECTED</li>
 *   <li>{@code shouldSkipCronRegistration_for_3112_payload} — 3112 跳 cron / 3101 注册</li>
 *   <li>{@code recorderStartThrows_lockReleased_andFailedReturned} — HIGH#1 锁泄漏防御</li>
 *   <li>{@code collectThrows_inFlightCountsReachMetrics} — MEDIUM#1 metrics 不丢</li>
 *   <li>{@code recorderCompleteThrows_resultStillReturned} — MEDIUM#3 complete 异常不掩盖结果</li>
 *   <li>{@code errorMessageTrimmedToMaxLength} — MEDIUM#2 errorMessage 1024 截断</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CollectorSchedulerTest {

    private static final String ADAPTER_ID = "ADP_TEST";
    private static final String PAYLOAD_DT = "INVOICE_CONTRACT_3101";
    private static final String CRON = "0 0 * * * *";
    private static final long LOCK_TTL = 60_000L;

    private TaskScheduler taskScheduler;
    private CollectorProperties props;
    private PayloadAssembler assembler;
    private OutboundMessageEnqueuePort enqueuePort;
    private CollectionRunRecorder recorder;
    private DistributedLock lock;
    private CollectionMetrics metrics;

    @BeforeEach
    void setUp() {
        taskScheduler = mock(TaskScheduler.class);
        props = new CollectorProperties();
        props.setLockTtlMillis(LOCK_TTL);
        props.setBatchSize(500);
        props.setInstitutionCode("BANK_001");
        assembler = mock(PayloadAssembler.class);
        enqueuePort = mock(OutboundMessageEnqueuePort.class);
        recorder = mock(CollectionRunRecorder.class);
        lock = mock(DistributedLock.class);
        metrics = new CollectionMetrics();
    }

    @Test
    void success_allRecordsSubmitted_returnsSuccessAndAdvancesAcknowledge() {
        // given: enabled adapter, lock acquired, 3 records all assemble + submit OK
        configureAdapter(ADAPTER_ID, CRON, true, PAYLOAD_DT);
        CollectorAdapter adapter = stubAdapter(ADAPTER_ID, recordsOf(3));
        whenLockAcquired();
        whenAssembleReturnsEnvelope();
        whenSubmitReturns(EnqueueResult.Status.ENQUEUED);

        CollectorScheduler scheduler = newScheduler(adapter);

        // when
        CollectionRunResult result = scheduler.triggerManually(ADAPTER_ID);

        // then
        assertThat(result.status()).isEqualTo(CollectionRunResult.Status.SUCCESS);
        assertThat(result.assembled()).isEqualTo(3);
        assertThat(result.submitted()).isEqualTo(3);
        assertThat(result.errors()).isZero();
        assertThat(result.runId()).hasSize(32);
        assertThat(result.errorMessage()).isNull();
        verify(recorder, times(1)).start(eq(result.runId()), eq(ADAPTER_ID),
                eq(TriggerType.MANUAL), any(Instant.class));
        // T10 Simplify Q-2 fix: complete() takes (collected, assembled, submitted, errors).
        // 3 records collected, all succeed: collected=assembled=submitted=3, errors=0.
        verify(recorder, times(1)).complete(eq(result.runId()),
                eq(CollectionRunResult.Status.SUCCESS), eq(3), eq(3), eq(3), eq(0),
                eq(null), any(Instant.class));
        verify(lock, times(1)).release(any(LockToken.class));
    }

    @Test
    void skippedLock_tryLockEmpty_returnsSkippedAndIncrementsMetric() {
        configureAdapter(ADAPTER_ID, CRON, true, PAYLOAD_DT);
        CollectorAdapter adapter = stubAdapter(ADAPTER_ID, List.of());
        when(lock.tryLock(eq("RUN_" + ADAPTER_ID), anyLong())).thenReturn(Optional.empty());

        CollectorScheduler scheduler = newScheduler(adapter);

        CollectionRunResult result = scheduler.triggerManually(ADAPTER_ID);

        assertThat(result.status()).isEqualTo(CollectionRunResult.Status.SKIPPED);
        assertThat(result.runId()).isNull();
        assertThat(result.assembled()).isZero();
        assertThat(result.submitted()).isZero();
        assertThat(result.errors()).isZero();

        CollectionMetricsSnapshot snap = metrics.snapshot();
        assertThat(snap.skipped()).isEqualTo(1L);

        verify(recorder, never()).start(anyString(), anyString(), any(TriggerType.class), any(Instant.class));
        // T10 Simplify Q-2 fix: complete() now has 4 int params (collected, assembled, submitted, errors).
        verify(recorder, never()).complete(anyString(), any(CollectionRunResult.Status.class),
                any(Integer.class), any(Integer.class), any(Integer.class), any(Integer.class),
                any(), any(Instant.class));
        verify(lock, never()).release(any(LockToken.class));
    }

    @Test
    void partial_oneAssembleFails_oneDupKey_oneSuccess_returnsPartialWithErrorsOne() {
        configureAdapter(ADAPTER_ID, CRON, true, PAYLOAD_DT);
        List<CollectionRecord> records = recordsOf(3);
        CollectorAdapter adapter = stubAdapter(ADAPTER_ID, records);
        whenLockAcquired();
        // record 0 → assemble fails (RuntimeException)
        // record 1 → assemble OK, submit throws COLLECT_DUPLICATE_KEY
        // record 2 → assemble OK, submit OK
        when(assembler.assemble(records.get(0)))
                .thenThrow(new FepBusinessException(FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                        "missing required field for 3101: contractNo"));
        OutboundMessageEnvelope envelope = sampleEnvelope();
        when(assembler.assemble(records.get(1))).thenReturn(envelope);
        when(assembler.assemble(records.get(2))).thenReturn(envelope);
        when(enqueuePort.submit(envelope))
                .thenThrow(new FepBusinessException(FepErrorCode.COLLECT_DUPLICATE_KEY, "dup"))
                .thenReturn(new EnqueueResult("Q-2", EnqueueResult.Status.ENQUEUED));

        CollectorScheduler scheduler = newScheduler(adapter);

        CollectionRunResult result = scheduler.triggerManually(ADAPTER_ID);

        assertThat(result.status()).isEqualTo(CollectionRunResult.Status.PARTIAL);
        assertThat(result.assembled()).isEqualTo(2);   // dup + success
        assertThat(result.submitted()).isEqualTo(2);   // dup counts as submitted
        assertThat(result.errors()).isEqualTo(1);
        assertThat(result.errorMessage()).contains("missing required field for 3101: contractNo");
        // T10 Simplify Q-2 fix: 3 records collected (assembled=2 + 1 failure), assembled=2, submitted=2, errors=1.
        verify(recorder, times(1)).complete(eq(result.runId()),
                eq(CollectionRunResult.Status.PARTIAL), eq(3), eq(2), eq(2), eq(1),
                anyString(), any(Instant.class));
        verify(lock, times(1)).release(any(LockToken.class));
    }

    @Test
    void duplicate_allDupKey_returnsSuccessWithoutErrors() {
        configureAdapter(ADAPTER_ID, CRON, true, PAYLOAD_DT);
        List<CollectionRecord> records = recordsOf(3);
        CollectorAdapter adapter = stubAdapter(ADAPTER_ID, records);
        whenLockAcquired();
        whenAssembleReturnsEnvelope();
        when(enqueuePort.submit(any(OutboundMessageEnvelope.class)))
                .thenThrow(new FepBusinessException(FepErrorCode.COLLECT_DUPLICATE_KEY, "dup"));

        CollectorScheduler scheduler = newScheduler(adapter);

        CollectionRunResult result = scheduler.triggerManually(ADAPTER_ID);

        assertThat(result.status()).isEqualTo(CollectionRunResult.Status.SUCCESS);
        assertThat(result.assembled()).isEqualTo(3);
        assertThat(result.submitted()).isEqualTo(3);
        assertThat(result.errors()).isZero();
        assertThat(result.errorMessage()).isNull();
        // T10 Simplify Q-2 fix: 3 records collected, all dedupped → assembled=3, submitted=3, errors=0.
        verify(recorder, times(1)).complete(eq(result.runId()),
                eq(CollectionRunResult.Status.SUCCESS), eq(3), eq(3), eq(3), eq(0),
                eq(null), any(Instant.class));
    }

    @Test
    void triggerRejected_adapterDisabled_throwsCollectTriggerRejected() {
        // adapter present in props BUT enabled=false
        configureAdapter(ADAPTER_ID, CRON, false, PAYLOAD_DT);
        CollectorAdapter adapter = stubAdapter(ADAPTER_ID, List.of());
        CollectorScheduler scheduler = newScheduler(adapter);

        assertThatThrownBy(() -> scheduler.triggerManually(ADAPTER_ID))
                .isInstanceOf(FepBusinessException.class)
                .satisfies(ex -> assertThat(((FepBusinessException) ex).getErrorCode())
                        .isEqualTo(FepErrorCode.COLLECT_TRIGGER_REJECTED));

        verify(lock, never()).tryLock(anyString(), anyLong());
        verify(recorder, never()).start(anyString(), anyString(), any(TriggerType.class), any(Instant.class));
    }

    @Test
    void shouldSkipCronRegistration_for_3112_payload() {
        // two adapters: 3112 (skip) + 3101 (register)
        configureAdapter("ADP_3112", CRON, true, "CORE_ENT_CREDIT_3112");
        configureAdapter("ADP_3101", CRON, true, "INVOICE_CONTRACT_3101");
        CollectorAdapter adapter3112 = stubAdapter("ADP_3112", List.of());
        CollectorAdapter adapter3101 = stubAdapter("ADP_3101", List.of());

        CollectorScheduler scheduler = new CollectorScheduler(
                taskScheduler, props,
                List.of(adapter3112, adapter3101),
                assembler, enqueuePort, recorder, lock, metrics);

        scheduler.registerScheduledTasks();

        // only 3101 registered with TaskScheduler
        verify(taskScheduler, times(1)).schedule(any(Runnable.class), any(CronTrigger.class));
    }

    @Test
    void recorderStartThrows_lockReleased_andFailedReturned() {
        // HIGH #1: recorder.start throws (DB outage) — lock must still release.
        configureAdapter(ADAPTER_ID, CRON, true, PAYLOAD_DT);
        CollectorAdapter adapter = stubAdapter(ADAPTER_ID, List.of());
        whenLockAcquired();
        doThrow(new IllegalStateException("DB outage"))
                .when(recorder).start(anyString(), anyString(), any(TriggerType.class), any(Instant.class));

        CollectorScheduler scheduler = newScheduler(adapter);

        CollectionRunResult result = scheduler.triggerManually(ADAPTER_ID);

        assertThat(result.status()).isEqualTo(CollectionRunResult.Status.FAILED);
        assertThat(result.errors()).isEqualTo(1);
        assertThat(result.assembled()).isZero();
        assertThat(result.submitted()).isZero();
        assertThat(result.errorMessage()).contains("DB outage");
        // CRITICAL: lock MUST be released even though recorder.start failed
        verify(lock, times(1)).release(any(LockToken.class));
        // recorder.complete should NOT be called because no RUNNING row was persisted
        // T10 Simplify Q-2 fix: 4 int args now (collected, assembled, submitted, errors).
        verify(recorder, never()).complete(anyString(), any(CollectionRunResult.Status.class),
                anyInt(), anyInt(), anyInt(), anyInt(), any(), any(Instant.class));
    }

    @Test
    void collectThrows_inFlightCountsReachMetrics() {
        // MEDIUM #1: when collect() throws, in-flight per-record counts must reach metrics.
        configureAdapter(ADAPTER_ID, CRON, true, PAYLOAD_DT);
        CollectorAdapter adapter = mock(CollectorAdapter.class);
        when(adapter.getId()).thenReturn(ADAPTER_ID);
        when(adapter.getType()).thenReturn(AdapterType.JDBC);
        when(adapter.collect(any(CollectionRunContext.class)))
                .thenThrow(new IllegalStateException("source unavailable"));
        whenLockAcquired();

        CollectorScheduler scheduler = newScheduler(adapter);

        CollectionRunResult result = scheduler.triggerManually(ADAPTER_ID);

        assertThat(result.status()).isEqualTo(CollectionRunResult.Status.FAILED);
        assertThat(result.errors()).isEqualTo(1);
        assertThat(result.errorMessage()).contains("source unavailable");
        // Metrics: collect-failure increments incFailed(1)
        CollectionMetricsSnapshot snap = metrics.snapshot();
        assertThat(snap.failed()).isEqualTo(1L);
        verify(lock, times(1)).release(any(LockToken.class));
        // T10 Simplify Q-2 fix: collect() threw before assignment → collected=0, assembled=0, submitted=0, errors=1.
        verify(recorder, times(1)).complete(anyString(), eq(CollectionRunResult.Status.FAILED),
                eq(0), eq(0), eq(0), eq(1), anyString(), any(Instant.class));
    }

    @Test
    void recorderCompleteThrows_resultStillReturned() {
        // MEDIUM #3: recorder.complete throwing must NOT mask the orchestration result.
        configureAdapter(ADAPTER_ID, CRON, true, PAYLOAD_DT);
        CollectorAdapter adapter = stubAdapter(ADAPTER_ID, recordsOf(2));
        whenLockAcquired();
        whenAssembleReturnsEnvelope();
        whenSubmitReturns(EnqueueResult.Status.ENQUEUED);
        doThrow(new IllegalStateException("DB outage during complete"))
                .when(recorder).complete(anyString(), any(CollectionRunResult.Status.class),
                        // T10 Simplify Q-2 fix: 4 int args now (collected, assembled, submitted, errors).
                        anyInt(), anyInt(), anyInt(), anyInt(), any(), any(Instant.class));

        CollectorScheduler scheduler = newScheduler(adapter);

        // recorder.complete throws — but result must still be returned (suppressed)
        CollectionRunResult result = scheduler.triggerManually(ADAPTER_ID);

        assertThat(result.status()).isEqualTo(CollectionRunResult.Status.SUCCESS);
        assertThat(result.assembled()).isEqualTo(2);
        assertThat(result.submitted()).isEqualTo(2);
        assertThat(result.errors()).isZero();
        verify(lock, times(1)).release(any(LockToken.class));
    }

    @Test
    void errorMessageTrimmedToMaxLength() {
        // MEDIUM #2: errorMessage must be capped at 1024 chars to avoid V19 schema column overflow.
        configureAdapter(ADAPTER_ID, CRON, true, PAYLOAD_DT);
        List<CollectionRecord> records = recordsOf(1);
        CollectorAdapter adapter = stubAdapter(ADAPTER_ID, records);
        whenLockAcquired();
        String hugeMessage = "X".repeat(2048);  // > 1024 cap
        when(assembler.assemble(records.get(0)))
                .thenThrow(new FepBusinessException(FepErrorCode.COLLECT_ASSEMBLE_FAILURE, hugeMessage));

        CollectorScheduler scheduler = newScheduler(adapter);

        CollectionRunResult result = scheduler.triggerManually(ADAPTER_ID);

        assertThat(result.errorMessage()).hasSizeLessThanOrEqualTo(1024);
        assertThat(result.errorMessage()).endsWith("...");  // truncation marker
    }

    // ---- helpers ----

    private CollectorScheduler newScheduler(final CollectorAdapter adapter) {
        return new CollectorScheduler(
                taskScheduler, props,
                List.of(adapter),
                assembler, enqueuePort, recorder, lock, metrics);
    }

    private void configureAdapter(final String id, final String cron,
                                  final boolean enabled, final String payloadDataType) {
        CollectorProperties.Adapter cfg = new CollectorProperties.Adapter();
        cfg.setId(id);
        cfg.setType(AdapterType.JDBC.name());
        cfg.setCron(cron);
        cfg.setEnabled(enabled);
        cfg.setPayloadDataType(payloadDataType);
        props.getAdapters().add(cfg);
    }

    private CollectorAdapter stubAdapter(final String id, final List<CollectionRecord> records) {
        CollectorAdapter adapter = mock(CollectorAdapter.class);
        when(adapter.getId()).thenReturn(id);
        when(adapter.getType()).thenReturn(AdapterType.JDBC);
        when(adapter.collect(any(CollectionRunContext.class))).thenReturn(records);
        return adapter;
    }

    private void whenLockAcquired() {
        when(lock.tryLock(anyString(), anyLong())).thenReturn(Optional.of(
                new LockToken("RUN_" + ADAPTER_ID, "tok-1", System.currentTimeMillis(), LOCK_TTL)));
    }

    private void whenAssembleReturnsEnvelope() {
        when(assembler.assemble(any(CollectionRecord.class))).thenReturn(sampleEnvelope());
    }

    private void whenSubmitReturns(final EnqueueResult.Status status) {
        when(enqueuePort.submit(any(OutboundMessageEnvelope.class)))
                .thenReturn(new EnqueueResult("Q-1", status));
    }

    private OutboundMessageEnvelope sampleEnvelope() {
        return new OutboundMessageEnvelope(
                "3101",
                Direction.OUTBOUND,
                "00000000000000000000000000000001",
                new OutboundHeadFields("BANK_001", "20260501", "00000001"),
                new Object(),
                PAYLOAD_DT,
                "row#1");
    }

    private List<CollectionRecord> recordsOf(final int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> CollectionRecord.builder()
                        .adapterId(ADAPTER_ID)
                        .sourceRef("row#" + i)
                        .payloadDataType(PAYLOAD_DT)
                        .rawData(java.util.Map.of("k", "v" + i))
                        .collectedAt(Instant.now())
                        .idempotencyKey(IdempotencyKeyGenerator.generate(ADAPTER_ID, "row#" + i))
                        .build())
                .toList();
    }
}

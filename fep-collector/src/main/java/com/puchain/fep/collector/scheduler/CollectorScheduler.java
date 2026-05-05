package com.puchain.fep.collector.scheduler;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.collector.assembler.PayloadAssembler;
import com.puchain.fep.collector.run.CollectionRunRecorder;
import com.puchain.fep.collector.support.CollectionMetrics;
import com.puchain.fep.collector.support.CollectionRecord;
import com.puchain.fep.collector.support.CollectionRunContext;
import com.puchain.fep.collector.support.CollectionRunResult;
import com.puchain.fep.collector.support.CollectorAdapter;
import com.puchain.fep.collector.support.DistributedLock;
import com.puchain.fep.collector.support.LockToken;
import com.puchain.fep.collector.support.TriggerType;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.processor.intake.port.OutboundMessageEnqueuePort;
import com.puchain.fep.processor.intake.port.OutboundMessageEnvelope;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 数据采集层调度核心（Plan §T6a）。
 *
 * <p><b>职责：</b>
 * <ol>
 *   <li>{@link #registerScheduledTasks()} — 启动期注册 cron 任务（PRD §2.2.2）</li>
 *   <li>{@link #triggerManually(String)} — 手动触发指定适配器（PRD §2.2.3）</li>
 *   <li>{@link #runAdapter(CollectorAdapter, TriggerType)} — 单次运行编排（采集 → 组装 → 入队 → 推水位）</li>
 * </ol>
 *
 * <p><b>关键约定：</b>
 * <ul>
 *   <li><b>3112 manual-only</b> — {@code payloadDataType} 含 {@code "3112"} 跳过 cron 注册（PRD §2.2.3）</li>
 *   <li><b>互斥</b> — 通过 {@link DistributedLock#tryLock(String, long)} 防同 adapter 并发；锁忙返回 {@link CollectionRunResult.Status#SKIPPED}</li>
 *   <li><b>幂等容忍</b> — 入队抛 {@link FepErrorCode#COLLECT_DUPLICATE_KEY} 视为已成功（advance watermark）</li>
 *   <li><b>禁吞异常</b> — 编排级捕获 {@link RuntimeException} 必转 FAILED 并记日志，由 finally 释放锁</li>
 * </ul>
 *
 * <p>本类为骨架实现（T6a），下游 Port 由后续 Task 实装：
 * <ul>
 *   <li>{@link PayloadAssembler} — T7b {@code DefaultPayloadAssembler}</li>
 *   <li>{@link OutboundMessageEnqueuePort} — T7a fep-web {@code JpaOutboundMessageEnqueueService}</li>
 *   <li>{@link CollectionRunRecorder} — T8 {@code JdbcCollectionRunRecorder} + V19 SQL</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class CollectorScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(CollectorScheduler.class);

    /** 分布式锁键前缀：{@code RUN_<adapterId>}。 */
    private static final String LOCK_KEY_PREFIX = "RUN_";

    /** 3112 manual-only 触发标识 token —— 出现在 payloadDataType 子串中即视为 3112 类型。 */
    private static final String PAYLOAD_3112_TOKEN = "3112";

    /** errorMessage 最大长度（字符数）—— 与 V19 schema 列宽对齐，避免 DB 溢出。 */
    private static final int ERROR_MESSAGE_MAX_LENGTH = 1024;

    /** errorMessage 截断标记 {@code "..."} 长度（3 字符），用于 substring 末尾追加。 */
    private static final int ERROR_MESSAGE_ELLIPSIS_LENGTH = 3;

    private final TaskScheduler taskScheduler;
    private final CollectorProperties props;
    private final List<CollectorAdapter> adapters;
    private final PayloadAssembler assembler;
    private final OutboundMessageEnqueuePort enqueuePort;
    private final CollectionRunRecorder recorder;
    private final DistributedLock lock;
    private final CollectionMetrics metrics;

    /** 启动期一次性构造的 adapterId → adapter bean 映射，O(1) lookup。 */
    private final Map<String, CollectorAdapter> adaptersById;

    /**
     * 构造调度器（手工 wiring，不绑定 Spring 注解）。
     *
     * @param taskScheduler Spring {@link TaskScheduler}（非 null）
     * @param props         数据采集层配置（非 null）
     * @param adapters      Spring 注入的所有 {@link CollectorAdapter} bean 列表（非 null，可为空）
     * @param assembler     报文组装 Port（非 null）
     * @param enqueuePort   出站入队 Port（非 null）
     * @param recorder      采集运行记账 Port（非 null）
     * @param lock          分布式锁（非 null）
     * @param metrics       指标聚合器（非 null）
     */
    public CollectorScheduler(final TaskScheduler taskScheduler,
                              final CollectorProperties props,
                              final List<CollectorAdapter> adapters,
                              final PayloadAssembler assembler,
                              final OutboundMessageEnqueuePort enqueuePort,
                              final CollectionRunRecorder recorder,
                              final DistributedLock lock,
                              final CollectionMetrics metrics) {
        this.taskScheduler = Objects.requireNonNull(taskScheduler, "taskScheduler");
        this.props = Objects.requireNonNull(props, "props");
        this.adapters = List.copyOf(Objects.requireNonNull(adapters, "adapters"));
        this.assembler = Objects.requireNonNull(assembler, "assembler");
        this.enqueuePort = Objects.requireNonNull(enqueuePort, "enqueuePort");
        this.recorder = Objects.requireNonNull(recorder, "recorder");
        this.lock = Objects.requireNonNull(lock, "lock");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.adaptersById = buildIndex(this.adapters);
    }

    private static Map<String, CollectorAdapter> buildIndex(final List<CollectorAdapter> adapters) {
        Map<String, CollectorAdapter> index = new HashMap<>(adapters.size() * 2);
        for (CollectorAdapter adapter : adapters) {
            index.put(adapter.getId(), adapter);
        }
        return Map.copyOf(index);
    }

    /**
     * 启动期注册 cron 任务（PRD §2.2.2）。
     *
     * <p>跳过条件（任一命中）：
     * <ul>
     *   <li>{@code enabled=false}</li>
     *   <li>{@code cron} 为空 / 空白</li>
     *   <li>{@code payloadDataType} 含 {@link #PAYLOAD_3112_TOKEN}（PRD §2.2.3 manual-only）</li>
     *   <li>对应 adapter bean 未注册（配置漂移防御）</li>
     * </ul>
     */
    @PostConstruct
    public void registerScheduledTasks() {
        for (CollectorProperties.Adapter cfg : props.getAdapters()) {
            if (!cfg.isEnabled()) {
                continue;
            }
            String cron = cfg.getCron();
            if (cron == null || cron.isBlank()) {
                continue;
            }
            String payloadDataType = cfg.getPayloadDataType();
            if (payloadDataType != null && payloadDataType.contains(PAYLOAD_3112_TOKEN)) {
                LOG.info("3112 manual-only per PRD §2.2.3, skip cron registration: adapterId={}",
                        LogSanitizer.sanitize(cfg.getId()));
                continue;
            }
            CollectorAdapter adapter = adaptersById.get(cfg.getId());
            if (adapter == null) {
                LOG.warn("adapter bean missing for configured adapterId={}, skip cron registration",
                        LogSanitizer.sanitize(cfg.getId()));
                continue;
            }
            taskScheduler.schedule(() -> runAdapter(adapter, TriggerType.SCHEDULED), new CronTrigger(cron));
        }
    }

    /**
     * 手动触发指定适配器（PRD §2.2.3 / Plan §T6 #4）。
     *
     * @param adapterId 适配器 ID（非 null / 非空）
     * @return 运行结果（{@link CollectionRunResult}）
     * @throws FepBusinessException 当 adapterId 不存在 / 已禁用 / adapter bean 缺失时
     *         （错误码 {@link FepErrorCode#COLLECT_TRIGGER_REJECTED}）
     */
    public CollectionRunResult triggerManually(final String adapterId) {
        Objects.requireNonNull(adapterId, "adapterId");
        CollectorProperties.Adapter cfg = findCfg(adapterId);
        if (cfg == null || !cfg.isEnabled()) {
            throw new FepBusinessException(FepErrorCode.COLLECT_TRIGGER_REJECTED,
                    "adapter not found or disabled: " + LogSanitizer.sanitize(adapterId));
        }
        CollectorAdapter adapter = adaptersById.get(adapterId);
        if (adapter == null) {
            throw new FepBusinessException(FepErrorCode.COLLECT_TRIGGER_REJECTED,
                    "adapter bean missing for adapterId=" + LogSanitizer.sanitize(adapterId));
        }
        return runAdapter(adapter, TriggerType.MANUAL);
    }

    /**
     * 单次运行编排（Plan §T6 #5）。
     *
     * <p>顺序：tryLock → 写 RUNNING → collect → 逐条 assemble + submit → acknowledge →
     * 写 终态 → release lock。COLLECT_DUPLICATE_KEY 视为已成功（advance watermark）；
     * 其他异常计 error 并跳过该条，最终 errors=0/部分/全部 决定 SUCCESS/PARTIAL/FAILED。
     *
     * <p><b>T6a-fix 防御加固</b>：
     * <ul>
     *   <li>HIGH#1：{@code recorder.start} 移入 try 块，失败时合成 FAILED 结果，锁仍释放</li>
     *   <li>MEDIUM#1：collect/orchestration 异常时，in-flight per-record counts 累加进 metrics</li>
     *   <li>MEDIUM#2：errorMessage 经 {@link #trimErrorMessage(String)} 截断至 1024 字符</li>
     *   <li>MEDIUM#3：{@code recorder.complete} 异常被 {@link #safeRecorderComplete} 吞掉以保留原始结果</li>
     * </ul>
     *
     * @param adapter     目标适配器（非 null）
     * @param triggerType 触发方式（非 null）
     * @return 运行结果
     */
    public CollectionRunResult runAdapter(final CollectorAdapter adapter, final TriggerType triggerType) {
        Objects.requireNonNull(adapter, "adapter");
        Objects.requireNonNull(triggerType, "triggerType");
        String adapterId = adapter.getId();
        String lockKey = LOCK_KEY_PREFIX + adapterId;
        Optional<LockToken> token = lock.tryLock(lockKey, props.getLockTtlMillis());
        if (token.isEmpty()) {
            metrics.incSkipped(1L);
            LOG.info("collection run skipped, lock busy: adapterId={}",
                    LogSanitizer.sanitize(adapterId));
            return CollectionRunResult.skipped(adapterId);
        }

        // Lock held — MUST release in finally regardless of how we exit.
        String runId = null;
        Counts counts = new Counts();
        List<CollectionRecord> processed = new ArrayList<>();
        // T10 Simplify Q-2 fix: track raw adapter.collect() count outside try so the
        // catch block can pass it to recorder.complete (collected stays 0 if collect()
        // itself threw — the FAILED status is the correct semantic in that case).
        int collected = 0;
        try {
            runId = IdGenerator.uuid32();
            Instant startedAt = Instant.now();
            try {
                recorder.start(runId, adapterId, triggerType, startedAt);
            } catch (RuntimeException startEx) {
                // HIGH#1: recorder.start failure cannot persist RUNNING row.
                // Synthesize FAILED CollectionRunResult; lock will release in outer finally.
                String startErrMsg = trimErrorMessage("collection run start failed: " + startEx.getMessage());
                LOG.error("collection run start failed (recorder.start threw): adapterId={}",
                        LogSanitizer.sanitize(adapterId), startEx);
                metrics.incFailed(1L);
                return new CollectionRunResult(runId, adapterId, CollectionRunResult.Status.FAILED,
                        0, 0, 1, startErrMsg);
            }
            CollectionRunContext ctx = new CollectionRunContext(
                    runId, adapterId, triggerType,
                    Optional.empty(),
                    startedAt,
                    props.getBatchSize());
            List<CollectionRecord> records = adapter.collect(ctx);
            collected = records.size();
            for (CollectionRecord rec : records) {
                processOneRecord(adapterId, rec, counts, processed);
            }
            adapter.acknowledge(ctx, processed);

            metrics.incAssembled(counts.assembled);
            metrics.incSubmitted(counts.submitted);
            metrics.incFailed(counts.errors);

            CollectionRunResult.Status terminal = decideStatus(counts);
            safeRecorderComplete(runId, terminal, collected, counts.assembled, counts.submitted,
                    counts.errors, counts.firstError);
            return new CollectionRunResult(runId, adapterId, terminal,
                    counts.assembled, counts.submitted, counts.errors, counts.firstError);
        } catch (RuntimeException e) {
            // MEDIUM#1: include in-flight per-record counts in metrics; +1 for orchestration failure.
            metrics.incAssembled(counts.assembled);
            metrics.incSubmitted(counts.submitted);
            metrics.incFailed((long) (counts.errors + 1));
            String msg = trimErrorMessage("collection run failed: " + e.getMessage());
            int errorsTotal = counts.errors + 1;
            safeRecorderComplete(runId, CollectionRunResult.Status.FAILED,
                    collected, counts.assembled, counts.submitted, errorsTotal, msg);
            LOG.error("collection run failed: adapterId={}", LogSanitizer.sanitize(adapterId), e);
            return new CollectionRunResult(runId, adapterId, CollectionRunResult.Status.FAILED,
                    counts.assembled, counts.submitted, errorsTotal, msg);
        } finally {
            lock.release(token.get());
        }
    }

    private void processOneRecord(final String adapterId, final CollectionRecord rec,
                                  final Counts counts, final List<CollectionRecord> processed) {
        try {
            OutboundMessageEnvelope envelope = assembler.assemble(rec);
            enqueuePort.submit(envelope);
            counts.assembled++;
            counts.submitted++;
            processed.add(rec);
        } catch (FepBusinessException e) {
            if (e.getErrorCode() == FepErrorCode.COLLECT_DUPLICATE_KEY) {
                counts.assembled++;
                counts.submitted++;
                processed.add(rec);
                LOG.info("duplicate idempotency, treated as already submitted: "
                                + "adapterId={} sourceRef={}",
                        LogSanitizer.sanitize(adapterId),
                        LogSanitizer.sanitize(rec.getSourceRef()));
            } else {
                counts.errors++;
                if (counts.firstError == null) {
                    counts.firstError = trimErrorMessage(e.getMessage());
                }
                LOG.warn("record processing failed (business): adapterId={} sourceRef={} code={}",
                        LogSanitizer.sanitize(adapterId),
                        LogSanitizer.sanitize(rec.getSourceRef()),
                        e.getErrorCode().getCode(), e);
            }
        } catch (RuntimeException e) {
            counts.errors++;
            if (counts.firstError == null) {
                counts.firstError = trimErrorMessage(e.getMessage());
            }
            LOG.warn("record processing failed (unexpected): adapterId={} sourceRef={}",
                    LogSanitizer.sanitize(adapterId),
                    LogSanitizer.sanitize(rec.getSourceRef()), e);
        }
    }

    /**
     * 包装 {@link CollectionRunRecorder#complete} 调用，吞掉 RuntimeException
     * 以保留原始编排结果（MEDIUM#3）。
     *
     * <p>{@code runId == null} 表示 {@code recorder.start} 已失败、未持久化 RUNNING 行，
     * 此时直接跳过 complete 调用。
     *
     * @param runId        运行 ID，可为 {@code null}（start 失败场景）
     * @param status       终态
     * @param collected    {@code adapter.collect} 返回的原始记录条数（T10 Simplify Q-2 fix）
     * @param assembled    组装成功条数
     * @param submitted    入队成功条数
     * @param errors       错误条数
     * @param errorMessage 首个错误消息（已截断）
     */
    private void safeRecorderComplete(final String runId, final CollectionRunResult.Status status,
                                      final int collected, final int assembled, final int submitted,
                                      final int errors, final String errorMessage) {
        if (runId == null) {
            // start path failed earlier — no RUNNING row to complete; nothing to do.
            return;
        }
        try {
            recorder.complete(runId, status, collected, assembled, submitted, errors, errorMessage, Instant.now());
        } catch (RuntimeException persistEx) {
            // MEDIUM#3: recorder.complete failure must NOT mask the orchestration result.
            // Log and suppress; CollectionRunResult is still returned to caller.
            LOG.error("recorder.complete failed (suppressed to preserve original result): "
                            + "runId={} status={}",
                    LogSanitizer.sanitize(runId), status.name(), persistEx);
        }
    }

    /**
     * 截断错误消息至 {@value #ERROR_MESSAGE_MAX_LENGTH} 字符，避免 V19 schema 列宽溢出（MEDIUM#2）。
     *
     * <p>超出长度时尾部追加 {@code "..."} 作为截断标记。
     *
     * @param msg 原始错误消息（可为 {@code null}）
     * @return 截断后消息，{@code null} 透传
     */
    private static String trimErrorMessage(final String msg) {
        if (msg == null) {
            return null;
        }
        if (msg.length() <= ERROR_MESSAGE_MAX_LENGTH) {
            return msg;
        }
        return msg.substring(0, ERROR_MESSAGE_MAX_LENGTH - ERROR_MESSAGE_ELLIPSIS_LENGTH) + "...";
    }

    private static CollectionRunResult.Status decideStatus(final Counts c) {
        if (c.errors == 0) {
            return CollectionRunResult.Status.SUCCESS;
        }
        if (c.submitted > 0) {
            return CollectionRunResult.Status.PARTIAL;
        }
        return CollectionRunResult.Status.FAILED;
    }

    private CollectorProperties.Adapter findCfg(final String adapterId) {
        for (CollectorProperties.Adapter cfg : props.getAdapters()) {
            if (adapterId.equals(cfg.getId())) {
                return cfg;
            }
        }
        return null;
    }

    /** 局部可变计数容器 — 仅本类使用，避免 6 个 int/String 参数串走方法签名。 */
    private static final class Counts {
        private int assembled;
        private int submitted;
        private int errors;
        private String firstError;
    }
}

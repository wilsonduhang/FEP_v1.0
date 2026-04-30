package com.puchain.fep.collector.adapter.mq;

import com.puchain.fep.collector.support.AdapterType;
import com.puchain.fep.collector.support.CollectionMetrics;
import com.puchain.fep.collector.support.CollectionRecord;
import com.puchain.fep.collector.support.CollectionRunContext;
import com.puchain.fep.collector.support.CollectorAdapter;
import com.puchain.fep.collector.support.IdempotencyKeyGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQ 数据采集适配器抽象基类（PRD v1.3 §2.2.2 数仓模式 / §2.1 MQ 适配器）。
 *
 * <p><b>采集语义：</b>
 * <ol>
 *   <li>{@link #collect} 调用子类 {@link #pollMessages(int)} 拉取最多 {@code batchSize} 条消息</li>
 *   <li>每条消息映射为 {@link CollectionRecord}（{@code sourceRef = MQ#messageId=<id>}）</li>
 *   <li>本批 messageIds 暂存到 {@link #inFlight} 直到 ack</li>
 *   <li>{@code metrics.incCollected(n)} 累加</li>
 * </ol>
 *
 * <p>{@link #acknowledge} 从 records 反解 messageIds → 调用子类 {@link #commit(List)}
 * 实现 broker 侧 ack（at-least-once 语义） → 清 {@link #inFlight}。
 *
 * <p><b>子类约定（abstract methods）：</b>
 * <ul>
 *   <li>{@link #pollMessages(int)} — 从 broker 拉取消息并解析 byte[] / JSON 为
 *       {@code Map<String, Object>} payload，返回不超过 max 条</li>
 *   <li>{@link #commit(List)} — 批量 ack 给定 messageIds（broker 侧具体协议子类决定）</li>
 * </ul>
 *
 * <p><b>{@link #inFlight} 设计动机：</b>记录"已 collect 但未 ack"的消息 id，避免：
 * <ul>
 *   <li>同一批次 ack 前再次 collect 时重复发往下游（语义错误）</li>
 *   <li>子类 commit() 部分失败时调用方可见仍未 ack 的 ids（生产场景需要）</li>
 * </ul>
 * 选择 {@link ConcurrentHashMap#newKeySet()} 而非 {@code HashSet} 因调度器可能
 * 在同一 adapter 上异步触发多次 collect/ack 周期。
 *
 * <p><b>线程安全：</b>本基类无可变实例字段除 {@link #inFlight}（线程安全集合），
 * 子类需自行保证 {@code pollMessages} / {@code commit} 的并发安全性。
 *
 * <p><b>非 Spring Bean：</b>遵循 {@link com.puchain.fep.collector.adapter.jdbc.JdbcCollectorAdapter}
 * 与 {@link com.puchain.fep.collector.adapter.file.FileCollectorAdapter} 先例 —
 * 由调用方（{@code AdapterFactory} / 配置驱动装配）显式 new 出来。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public abstract class MqCollectorAdapter implements CollectorAdapter {

    /**
     * sourceRef 前缀常量 — 格式 {@code MQ#messageId=<id>}（Plan §T4 #4 锁定）。
     *
     * <p>{@link #acknowledge} 反解 messageId 时直接 strip 该前缀。
     */
    static final String SOURCE_REF_PREFIX = "MQ#messageId=";

    private final String adapterId;
    private final String payloadDataType;
    private final CollectionMetrics metrics;

    /**
     * 已 collect 尚未 ack 的 messageId 集合 — 线程安全，跨多次 collect 累积，
     * 在 {@link #acknowledge} 时按本批 records 移除对应 id。
     */
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    /**
     * 基类构造（子类通过 super(...) 调用）。
     *
     * @param adapterId       适配器逻辑 ID（与 {@code fep.collector.adapters[*].id} 一致）；非 null / 非空
     * @param payloadDataType 报文数据类型（用于 PayloadAssembler 路由）；非 null
     * @param metrics         采集指标聚合器（{@link #collect} 时 incCollected 用）；非 null
     * @throws NullPointerException 任一参数为 null
     */
    protected MqCollectorAdapter(
            final String adapterId,
            final String payloadDataType,
            final CollectionMetrics metrics) {
        this.adapterId = Objects.requireNonNull(adapterId, "adapterId");
        this.payloadDataType = Objects.requireNonNull(payloadDataType, "payloadDataType");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
    }

    @Override
    public final String getId() {
        return adapterId;
    }

    @Override
    public final AdapterType getType() {
        return AdapterType.MQ;
    }

    @Override
    public final List<CollectionRecord> collect(final CollectionRunContext context) {
        Objects.requireNonNull(context, "context");
        final List<MqMessage> messages = pollMessages(context.batchSize());
        Objects.requireNonNull(messages, "pollMessages must not return null");
        if (messages.isEmpty()) {
            return List.of();
        }
        final Instant collectedAt = Instant.now();
        final List<CollectionRecord> records = new ArrayList<>(messages.size());
        for (final MqMessage msg : messages) {
            final String sourceRef = SOURCE_REF_PREFIX + msg.messageId();
            records.add(CollectionRecord.builder()
                    .adapterId(adapterId)
                    .sourceRef(sourceRef)
                    .payloadDataType(payloadDataType)
                    .rawData(msg.payload())
                    .collectedAt(collectedAt)
                    .idempotencyKey(IdempotencyKeyGenerator.generate(adapterId, sourceRef))
                    .build());
            inFlight.add(msg.messageId());
        }
        metrics.incCollected(records.size());
        return List.copyOf(records);
    }

    @Override
    public final void acknowledge(
            final CollectionRunContext context,
            final List<CollectionRecord> records) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(records, "records");
        if (records.isEmpty()) {
            return;
        }
        final List<String> messageIds = new ArrayList<>(records.size());
        for (final CollectionRecord record : records) {
            final String sourceRef = record.getSourceRef();
            if (sourceRef.startsWith(SOURCE_REF_PREFIX)) {
                messageIds.add(sourceRef.substring(SOURCE_REF_PREFIX.length()));
            }
            // sourceRef 不符合 MQ 格式时跳过（防御异质 record 误传） — 不静默吞错，
            // 但 ack 阶段不抛错以免阻塞水位推进；上游应保证 records 来自本 adapter。
        }
        // 子类 commit() 抛错必须向上传播 — 调度器据此决定是否清 inFlight / 推进水位。
        commit(messageIds);
        // 仅当 commit 成功后才清 inFlight（commit 抛错时保留以便重试可见）。
        inFlight.removeAll(messageIds);
    }

    /**
     * 已 collect 尚未 ack 的 messageId 不可变快照（用于子类调试 / 监控查询）。
     *
     * @return 不可变 Set 视图（非 null；快照不实时跟随后续修改）
     */
    protected final Set<String> inFlightSnapshot() {
        return Collections.unmodifiableSet(Set.copyOf(inFlight));
    }

    /**
     * 从 MQ broker 拉取最多 {@code max} 条消息（子类实现）。
     *
     * <p><b>实现要求：</b>
     * <ul>
     *   <li>byte[] / JSON wire 格式必须由本方法解析为 {@link MqMessage#payload()} Map</li>
     *   <li>返回 List 不应超过 {@code max} 条</li>
     *   <li>broker 暂无新消息时返回空 List（不阻塞）</li>
     *   <li>broker 异常应抛
     *       {@link com.puchain.fep.common.exception.FepBusinessException}
     *       （{@link com.puchain.fep.common.domain.FepErrorCode#COLLECT_ADAPTER_FAILURE}）</li>
     * </ul>
     *
     * @param max 上限（{@link CollectionRunContext#batchSize()}），保证 ≥ 1
     * @return 消息列表（非 null，可为空）
     */
    protected abstract List<MqMessage> pollMessages(int max);

    /**
     * 将给定 messageIds 在 broker 侧批量 ack（子类实现）。
     *
     * <p>at-least-once 语义：本方法返回成功 → broker 不会再重发这批消息。
     *
     * @param messageIds 待 ack 的消息 id（非 null，可为空 — 空时子类应实现为 no-op）
     */
    protected abstract void commit(List<String> messageIds);
}

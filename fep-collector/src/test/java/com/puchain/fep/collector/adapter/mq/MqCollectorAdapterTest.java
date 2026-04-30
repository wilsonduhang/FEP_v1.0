package com.puchain.fep.collector.adapter.mq;

import com.puchain.fep.collector.support.AdapterType;
import com.puchain.fep.collector.support.CollectionMetrics;
import com.puchain.fep.collector.support.CollectionRecord;
import com.puchain.fep.collector.support.CollectionRunContext;
import com.puchain.fep.collector.support.TriggerType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link MqCollectorAdapter} 单元测试 — 通过 {@link InMemoryMqCollectorAdapter} 验证
 * 抽象基类的 collect/acknowledge 通用流程及子类契约。
 *
 * <p>覆盖 Plan §T4 验收标准：
 * <ul>
 *   <li>#1 — 子类实现 pollMessages / commit（InMemory 子类驱动）</li>
 *   <li>#2 — MqMessage record + null 防御</li>
 *   <li>#3 — InMemoryMqCollectorAdapter Deque + enqueueForTest 契约</li>
 *   <li>#4 — collect() 通用流程：poll → 转 CollectionRecord（sourceRef = MQ#messageId=...）→ 暂存 inFlight</li>
 *   <li>#5 — acknowledge() commit messageIds + 清 inFlight</li>
 *   <li>#6 — 端到端：投放 5 → poll 返回 5 + sourceRef 含 messageId → ack → 再 poll = 0</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class MqCollectorAdapterTest {

    private static final String ADAPTER_ID = "MQ_INVOICE_TEST";
    private static final String PAYLOAD_DATA_TYPE = "INVOICE_TEST_3101";
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final String SOURCE_REF_PREFIX = "MQ#messageId=";

    /**
     * Plan §T4 #6 — 投放 5 条 → collect → 5 records，每条 sourceRef 含 messageId。
     */
    @Test
    void shouldCollectAllEnqueuedMessages() {
        final CollectionMetrics metrics = new CollectionMetrics();
        final InMemoryMqCollectorAdapter adapter =
                new InMemoryMqCollectorAdapter(ADAPTER_ID, PAYLOAD_DATA_TYPE, metrics);

        for (int i = 1; i <= 5; i++) {
            adapter.enqueueForTest(new MqMessage(
                    "msg-" + i,
                    Map.of("orderNo", "O" + i, "amount", 100 * i),
                    Instant.now()));
        }

        final List<CollectionRecord> records = adapter.collect(ctx());

        assertThat(records)
                .as("Plan §T4 #6 — 5 条入队 → collect 返回 5 条记录")
                .hasSize(5);
        for (int i = 0; i < 5; i++) {
            final CollectionRecord r = records.get(i);
            final String expectedId = "msg-" + (i + 1);
            assertThat(r.getSourceRef())
                    .as("Plan §T4 #4 — sourceRef = MQ#messageId=<id>")
                    .isEqualTo(SOURCE_REF_PREFIX + expectedId)
                    .contains(expectedId);
            assertThat(r.getAdapterId()).isEqualTo(ADAPTER_ID);
            assertThat(r.getPayloadDataType()).isEqualTo(PAYLOAD_DATA_TYPE);
        }
    }

    /**
     * Plan §T4 #6 — collect → acknowledge → 再 collect = 0。
     */
    @Test
    void shouldNotRecollectAfterAcknowledge() {
        final InMemoryMqCollectorAdapter adapter = newAdapter();
        for (int i = 1; i <= 3; i++) {
            adapter.enqueueForTest(new MqMessage(
                    "msg-" + i, Map.of("k", "v" + i), Instant.now()));
        }

        final List<CollectionRecord> first = adapter.collect(ctx());
        assertThat(first).hasSize(3);
        adapter.acknowledge(ctx(), first);

        final List<CollectionRecord> second = adapter.collect(ctx());
        assertThat(second)
                .as("Plan §T4 #6 — ack 后队列已 drain，再次 collect 应为空")
                .isEmpty();
    }

    /**
     * Plan §T4 #2 — MqMessage payload 透传到 CollectionRecord.rawData（业务字段保真）。
     */
    @Test
    void shouldExposeRawDataAsPayload() {
        final InMemoryMqCollectorAdapter adapter = newAdapter();
        final Map<String, Object> payload = Map.of(
                "orderNo", "O001",
                "amount", 1000);
        adapter.enqueueForTest(new MqMessage("msg-1", payload, Instant.now()));

        final List<CollectionRecord> records = adapter.collect(ctx());

        assertThat(records).hasSize(1);
        assertThat(records.get(0).getRawData())
                .as("rawData 必须等于 enqueue 的 payload（不可丢字段 / 不可改字段）")
                .containsExactlyInAnyOrderEntriesOf(payload);
    }

    /**
     * Plan §T4 #4 — collect() 必须遵循 ctx.batchSize() — 队列 10 条、batch=3 → 返回 3 条。
     */
    @Test
    void shouldRespectBatchSize() {
        final InMemoryMqCollectorAdapter adapter = newAdapter();
        for (int i = 1; i <= 10; i++) {
            adapter.enqueueForTest(new MqMessage(
                    "msg-" + i, Map.of("k", "v"), Instant.now()));
        }

        final CollectionRunContext smallBatch = new CollectionRunContext(
                UUID.randomUUID().toString().replace("-", ""),
                ADAPTER_ID,
                TriggerType.SCHEDULED,
                Optional.empty(),
                Instant.now(),
                3);
        final List<CollectionRecord> records = adapter.collect(smallBatch);

        assertThat(records)
                .as("batchSize=3 上限严格遵循（剩余 7 条留在队列）")
                .hasSize(3);
    }

    /**
     * Plan §T4 #5 — acknowledge 必须把本批 messageIds 调用 commit() 落到子类。
     */
    @Test
    void acknowledgeShouldCommitMessageIds() {
        final InMemoryMqCollectorAdapter adapter = newAdapter();
        adapter.enqueueForTest(new MqMessage("msg-A", Map.of("k", "1"), Instant.now()));
        adapter.enqueueForTest(new MqMessage("msg-B", Map.of("k", "2"), Instant.now()));
        adapter.enqueueForTest(new MqMessage("msg-C", Map.of("k", "3"), Instant.now()));

        final List<CollectionRecord> records = adapter.collect(ctx());
        adapter.acknowledge(ctx(), records);

        assertThat(adapter.committedIds())
                .as("Plan §T4 #5 — ack 必须把 messageIds 透传给子类 commit()")
                .containsExactlyInAnyOrder("msg-A", "msg-B", "msg-C");
    }

    /**
     * Adapter 类型契约 — getType() = MQ；getId() echo 构造参数。
     */
    @Test
    void getTypeShouldReturnMq() {
        final InMemoryMqCollectorAdapter adapter = newAdapter();
        assertThat(adapter.getType())
                .as("MqCollectorAdapter.getType() 必须返回 AdapterType.MQ")
                .isEqualTo(AdapterType.MQ);
        assertThat(adapter.getId())
                .as("getId() 必须 echo 构造时传入的 adapterId")
                .isEqualTo(ADAPTER_ID);
    }

    /**
     * Plan §T4 #2 — MqMessage compact 构造函数 null 防御（messageId / payload / enqueuedAt）。
     */
    @Test
    void mqMessageShouldRejectNullFields() {
        assertThatThrownBy(() -> new MqMessage(null, Map.of(), Instant.now()))
                .as("messageId == null 必须抛 NPE")
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("messageId");

        assertThatThrownBy(() -> new MqMessage("id", null, Instant.now()))
                .as("payload == null 必须抛 NPE")
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("payload");

        assertThatThrownBy(() -> new MqMessage("id", Map.of(), null))
                .as("enqueuedAt == null 必须抛 NPE")
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("enqueuedAt");
    }

    /**
     * 同一 messageId 产生稳定 idempotencyKey（Plan §T4 #4 — IdempotencyKeyGenerator 协议保证）。
     */
    @Test
    void idempotencyKeyShouldBeStableForSameMessageId() {
        final InMemoryMqCollectorAdapter adapter = newAdapter();
        adapter.enqueueForTest(new MqMessage("msg-stable", Map.of("k", "v1"), Instant.now()));
        final List<CollectionRecord> first = adapter.collect(ctx());
        // 不 ack — 重投同 messageId（生产语义：MQ 重发 / DLQ 转入）
        adapter.enqueueForTest(new MqMessage("msg-stable", Map.of("k", "v2"), Instant.now()));
        final List<CollectionRecord> second = adapter.collect(ctx());

        assertThat(first).hasSize(1);
        assertThat(second).hasSize(1);
        assertThat(second.get(0).getIdempotencyKey())
                .as("同一 messageId 生成的 idempotencyKey 必须稳定（写入 DB 唯一索引去重）")
                .isEqualTo(first.get(0).getIdempotencyKey());
    }

    /**
     * inFlight 跟踪契约 — collect 后未 ack 的 messageIds 可观察；ack 后清空。
     * 通过 {@link InspectableMqCollectorAdapter} 暴露 {@code inFlightSnapshot()}（基类 protected）。
     */
    @Test
    void inFlightShouldTrackCollectedAndClearOnAck() {
        final InspectableMqCollectorAdapter adapter = new InspectableMqCollectorAdapter(
                ADAPTER_ID, PAYLOAD_DATA_TYPE, new CollectionMetrics());
        adapter.enqueueForTest(new MqMessage("msg-X", Map.of("k", "v"), Instant.now()));
        adapter.enqueueForTest(new MqMessage("msg-Y", Map.of("k", "v"), Instant.now()));

        final List<CollectionRecord> records = adapter.collect(ctx());
        assertThat(adapter.exposedInFlight())
                .as("collect 后 inFlight 必须含本批 messageIds（at-least-once 语义）")
                .containsExactlyInAnyOrder("msg-X", "msg-Y");

        adapter.acknowledge(ctx(), records);
        assertThat(adapter.exposedInFlight())
                .as("ack 成功后 inFlight 必须清空")
                .isEmpty();
    }

    /**
     * Plan §T4 #4 — collect() 必须 metrics.incCollected(records.size())。
     */
    @Test
    void metricsShouldIncrementOnCollect() {
        final CollectionMetrics metrics = new CollectionMetrics();
        final InMemoryMqCollectorAdapter adapter =
                new InMemoryMqCollectorAdapter(ADAPTER_ID, PAYLOAD_DATA_TYPE, metrics);
        for (int i = 1; i <= 5; i++) {
            adapter.enqueueForTest(new MqMessage("msg-" + i, Map.of("k", "v"), Instant.now()));
        }

        adapter.collect(ctx());

        assertThat(metrics.snapshot().collected())
                .as("metrics.collected 必须等于本批采集条数")
                .isEqualTo(5L);
    }

    private InMemoryMqCollectorAdapter newAdapter() {
        return new InMemoryMqCollectorAdapter(ADAPTER_ID, PAYLOAD_DATA_TYPE, new CollectionMetrics());
    }

    private CollectionRunContext ctx() {
        return new CollectionRunContext(
                UUID.randomUUID().toString().replace("-", ""),
                ADAPTER_ID,
                TriggerType.SCHEDULED,
                Optional.empty(),
                Instant.now(),
                DEFAULT_BATCH_SIZE);
    }

    /**
     * 测试专用子类 — 直接继承抽象基类 {@link MqCollectorAdapter}，暴露
     * {@code inFlightSnapshot()}（基类 protected）以便测试断言；同时实现
     * 与 {@link InMemoryMqCollectorAdapter} 等价的内存队列语义。
     *
     * <p>本类仅存在于测试包，生产代码不可见 — 验证基类对子类提供的可观察性 hook。
     */
    private static final class InspectableMqCollectorAdapter extends MqCollectorAdapter {

        private final java.util.Deque<MqMessage> queue = new java.util.concurrent.ConcurrentLinkedDeque<>();

        InspectableMqCollectorAdapter(
                final String adapterId,
                final String payloadDataType,
                final CollectionMetrics metrics) {
            super(adapterId, payloadDataType, metrics);
        }

        void enqueueForTest(final MqMessage msg) {
            queue.addLast(msg);
        }

        Set<String> exposedInFlight() {
            return inFlightSnapshot();
        }

        @Override
        protected List<MqMessage> pollMessages(final int max) {
            final java.util.List<MqMessage> drained = new java.util.ArrayList<>();
            for (int i = 0; i < max; i++) {
                final MqMessage m = queue.pollFirst();
                if (m == null) {
                    break;
                }
                drained.add(m);
            }
            return List.copyOf(drained);
        }

        @Override
        protected void commit(final List<String> messageIds) {
            // no-op — 仅验证 inFlight 跟踪/清空，不需要持久化 ack 状态
        }
    }
}

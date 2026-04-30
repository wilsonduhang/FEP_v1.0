package com.puchain.fep.collector.support;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link CollectionRunContext} 单元测试。
 *
 * <p>覆盖：
 * <ul>
 *   <li>所有字段构造正常（runId / adapterId / triggerType / previousWatermark /
 *       startedAt / batchSize）</li>
 *   <li>{@code previousWatermark} 为 {@link Optional#empty()} 允许</li>
 *   <li>每个引用字段 null 校验抛 NullPointerException</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CollectionRunContextTest {

    private static final String VALID_RUN_ID = "11112222333344445555666677778888";
    private static final String VALID_ADAPTER_ID = "ADP1";
    private static final Instant VALID_STARTED_AT = Instant.parse("2026-04-30T00:00:00Z");
    private static final int VALID_BATCH_SIZE = 500;

    @Test
    void shouldConstructWithAllFields() {
        CollectionRunContext ctx = new CollectionRunContext(
                VALID_RUN_ID,
                VALID_ADAPTER_ID,
                TriggerType.SCHEDULED,
                Optional.of("2026-04-29T23:59:59Z"),
                VALID_STARTED_AT,
                VALID_BATCH_SIZE);

        assertThat(ctx.runId()).isEqualTo(VALID_RUN_ID);
        assertThat(ctx.adapterId()).isEqualTo(VALID_ADAPTER_ID);
        assertThat(ctx.triggerType()).isEqualTo(TriggerType.SCHEDULED);
        assertThat(ctx.previousWatermark()).contains("2026-04-29T23:59:59Z");
        assertThat(ctx.startedAt()).isEqualTo(VALID_STARTED_AT);
        assertThat(ctx.batchSize()).isEqualTo(VALID_BATCH_SIZE);
    }

    @Test
    void shouldAllowEmptyPreviousWatermark() {
        CollectionRunContext ctx = new CollectionRunContext(
                VALID_RUN_ID,
                VALID_ADAPTER_ID,
                TriggerType.MANUAL,
                Optional.empty(),
                VALID_STARTED_AT,
                VALID_BATCH_SIZE);

        assertThat(ctx.previousWatermark())
                .as("首次采集场景 previousWatermark=Optional.empty 必须允许")
                .isEmpty();
    }

    @Test
    void shouldRejectNullRunId() {
        assertThatThrownBy(() -> new CollectionRunContext(
                null, VALID_ADAPTER_ID, TriggerType.SCHEDULED,
                Optional.empty(), VALID_STARTED_AT, VALID_BATCH_SIZE))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("runId");
    }

    @Test
    void shouldRejectNullAdapterId() {
        assertThatThrownBy(() -> new CollectionRunContext(
                VALID_RUN_ID, null, TriggerType.SCHEDULED,
                Optional.empty(), VALID_STARTED_AT, VALID_BATCH_SIZE))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("adapterId");
    }

    @Test
    void shouldRejectNullTriggerType() {
        assertThatThrownBy(() -> new CollectionRunContext(
                VALID_RUN_ID, VALID_ADAPTER_ID, null,
                Optional.empty(), VALID_STARTED_AT, VALID_BATCH_SIZE))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("triggerType");
    }

    @Test
    void shouldRejectNullPreviousWatermark() {
        assertThatThrownBy(() -> new CollectionRunContext(
                VALID_RUN_ID, VALID_ADAPTER_ID, TriggerType.SCHEDULED,
                null, VALID_STARTED_AT, VALID_BATCH_SIZE))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("previousWatermark");
    }

    @Test
    void shouldRejectNullStartedAt() {
        assertThatThrownBy(() -> new CollectionRunContext(
                VALID_RUN_ID, VALID_ADAPTER_ID, TriggerType.SCHEDULED,
                Optional.empty(), null, VALID_BATCH_SIZE))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("startedAt");
    }
}

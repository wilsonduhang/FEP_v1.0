package com.puchain.fep.collector.support;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link CollectionRecord} 单元测试。
 *
 * <p>覆盖：
 * <ul>
 *   <li>Builder 产出不可变实例 — 所有 getter 返回构造时入参</li>
 *   <li>{@code Map.copyOf} 防御 — 外部修改原始 map 不影响内部 rawData</li>
 *   <li>idempotencyKey 长度校验 — length=31 抛 IllegalArgumentException 且消息含
 *       "idempotencyKey" 与 "32"</li>
 *   <li>每个字段 null 校验抛 NullPointerException</li>
 *   <li>边界：空 rawData (Map.of()) 允许</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CollectionRecordTest {

    private static final String VALID_KEY_32 = "0123456789abcdef0123456789abcdef";
    private static final String VALID_KEY_31 = "0123456789abcdef0123456789abcde";

    private static CollectionRecord.Builder validBuilder() {
        return CollectionRecord.builder()
                .adapterId("ADP1")
                .sourceRef("row#42")
                .payloadDataType("INVOICE_CONTRACT_3101")
                .rawData(Map.of("k", "v"))
                .collectedAt(Instant.parse("2026-04-30T00:00:00Z"))
                .idempotencyKey(VALID_KEY_32);
    }

    @Test
    void builderShouldProduceInstanceWithAllFields() {
        Instant collectedAt = Instant.parse("2026-04-30T01:23:45Z");
        Map<String, Object> raw = Map.of("invoiceNo", "INV001", "amount", 12345L);

        CollectionRecord record = CollectionRecord.builder()
                .adapterId("ADP_JDBC_3101")
                .sourceRef("row#42")
                .payloadDataType("INVOICE_CONTRACT_3101")
                .rawData(raw)
                .collectedAt(collectedAt)
                .idempotencyKey(VALID_KEY_32)
                .build();

        assertThat(record.getAdapterId()).isEqualTo("ADP_JDBC_3101");
        assertThat(record.getSourceRef()).isEqualTo("row#42");
        assertThat(record.getPayloadDataType()).isEqualTo("INVOICE_CONTRACT_3101");
        assertThat(record.getRawData()).containsExactlyInAnyOrderEntriesOf(raw);
        assertThat(record.getCollectedAt()).isEqualTo(collectedAt);
        assertThat(record.getIdempotencyKey()).isEqualTo(VALID_KEY_32);
    }

    @Test
    void rawDataShouldBeDefensivelyCopiedFromExternalMutation() {
        Map<String, Object> mutable = new HashMap<>();
        mutable.put("k1", "v1");

        CollectionRecord record = CollectionRecord.builder()
                .adapterId("ADP1")
                .sourceRef("row#42")
                .payloadDataType("INVOICE_CONTRACT_3101")
                .rawData(mutable)
                .collectedAt(Instant.now())
                .idempotencyKey(VALID_KEY_32)
                .build();

        // 外部修改 — 不应影响内部
        mutable.put("k2", "v2");
        mutable.remove("k1");

        assertThat(record.getRawData())
                .as("Map.copyOf 防御：外部对原 map 的修改必须不影响内部 rawData")
                .containsExactly(Map.entry("k1", "v1"));
    }

    @Test
    void rawDataGetterShouldBeImmutable() {
        CollectionRecord record = validBuilder().build();

        assertThatThrownBy(() -> record.getRawData().put("evil", "value"))
                .as("Map.copyOf 返回不可变视图，外部不得通过 getter 篡改内部状态")
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectIdempotencyKeyShorterThan32() {
        assertThatThrownBy(() -> validBuilder().idempotencyKey(VALID_KEY_31).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idempotencyKey")
                .hasMessageContaining("32");
    }

    @Test
    void shouldRejectIdempotencyKeyLongerThan32() {
        String tooLong = VALID_KEY_32 + "0";

        assertThatThrownBy(() -> validBuilder().idempotencyKey(tooLong).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idempotencyKey")
                .hasMessageContaining("32");
    }

    @Test
    void shouldRejectNullAdapterId() {
        assertThatThrownBy(() -> validBuilder().adapterId(null).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("adapterId");
    }

    @Test
    void shouldRejectNullSourceRef() {
        assertThatThrownBy(() -> validBuilder().sourceRef(null).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("sourceRef");
    }

    @Test
    void shouldRejectNullPayloadDataType() {
        assertThatThrownBy(() -> validBuilder().payloadDataType(null).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("payloadDataType");
    }

    @Test
    void shouldRejectNullRawData() {
        assertThatThrownBy(() -> validBuilder().rawData(null).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("rawData");
    }

    @Test
    void shouldRejectNullCollectedAt() {
        assertThatThrownBy(() -> validBuilder().collectedAt(null).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("collectedAt");
    }

    @Test
    void shouldRejectNullIdempotencyKey() {
        assertThatThrownBy(() -> validBuilder().idempotencyKey(null).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("idempotencyKey");
    }

    @Test
    void shouldAllowEmptyRawData() {
        CollectionRecord record = validBuilder().rawData(Map.of()).build();

        assertThat(record.getRawData())
                .as("空 rawData 必须允许（部分适配器仅传元数据无业务字段）")
                .isEmpty();
    }
}

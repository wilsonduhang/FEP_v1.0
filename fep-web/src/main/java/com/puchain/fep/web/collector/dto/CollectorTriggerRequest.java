package com.puchain.fep.web.collector.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 手动触发数据采集运行的请求 DTO（PRD §2.2.3 + §5.5 报送管理）。
 *
 * <p>P4 T6b — {@code POST /api/v1/collector/triggers} 入参。{@code adapterId}
 * 由 {@code CollectorScheduler.triggerManually(adapterId)} 在配置（{@code fep.collector.adapters[].id}）
 * 中查找；缺失或 {@code enabled=false} → 抛 {@code COLLECT_TRIGGER_REJECTED}（HTTP 400）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Schema(description = "手动触发数据采集请求", name = "CollectorTriggerRequest")
public class CollectorTriggerRequest {

    /** adapter ID 上限，与 {@code CollectorProperties.Adapter.id} 字段约束对齐。 */
    private static final int ADAPTER_ID_MAX = 64;

    @Schema(description = "适配器 ID（必须匹配 fep.collector.adapters[].id）",
            example = "ADP_3101", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "adapterId 不能为空")
    @Size(max = ADAPTER_ID_MAX, message = "adapterId 长度不能超过 " + ADAPTER_ID_MAX)
    private String adapterId;

    /**
     * Default constructor for Jackson.
     */
    public CollectorTriggerRequest() {
        // Jackson
    }

    /**
     * Returns the adapter id to trigger.
     *
     * @return adapter id
     */
    public String getAdapterId() {
        return adapterId;
    }

    /**
     * Sets the adapter id.
     *
     * @param v adapter id
     */
    public void setAdapterId(final String v) {
        this.adapterId = v;
    }
}

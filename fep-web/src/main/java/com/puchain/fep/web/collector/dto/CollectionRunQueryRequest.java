package com.puchain.fep.web.collector.dto;

import com.puchain.fep.common.domain.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * 采集运行历史查询请求 DTO（PRD §5.5 报送管理）。
 *
 * <p>P4 T6b — {@code GET /api/v1/collector/runs} 入参，继承 {@link PageQuery}
 * 取得 1-based {@code pageNum} / {@code pageSize}（{@code @Min/@Max}
 * 校验由父类提供）。所有过滤字段为可选；service 层用 {@code Specification}
 * 拼装条件链（参 {@code CollectionRunQueryService}）。</p>
 *
 * <p><b>时间范围语义</b>：{@code from} / {@code to} 都是 {@link Instant}
 * （UTC），与 {@link com.puchain.fep.web.collector.CollectionRunEntity#getStartedAt()}
 * 字段类型对齐。Spring 默认支持 ISO-8601 字符串绑定（如 {@code 2026-04-30T10:00:00Z}）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Schema(description = "采集运行历史查询请求", name = "CollectionRunQueryRequest")
public class CollectionRunQueryRequest extends PageQuery {

    /** adapter ID 上限，与 {@link CollectorTriggerRequest} 同源约束。 */
    private static final int ADAPTER_ID_MAX = 64;

    /** status 字符串上限，与 V19 schema 列宽对齐。 */
    private static final int STATUS_MAX = 16;

    @Schema(description = "适配器 ID 过滤（可选；精确匹配）", example = "ADP_3101")
    @Size(max = ADAPTER_ID_MAX, message = "adapterId 长度不能超过 " + ADAPTER_ID_MAX)
    private String adapterId;

    @Schema(description = "运行状态过滤（可选；RUNNING / SUCCESS / PARTIAL / FAILED / SKIPPED）",
            example = "SUCCESS")
    @Size(max = STATUS_MAX, message = "status 长度不能超过 " + STATUS_MAX)
    private String status;

    @Schema(description = "起始时间（含；ISO-8601 UTC，可选）",
            example = "2026-04-30T00:00:00Z")
    private Instant from;

    @Schema(description = "截止时间（含；ISO-8601 UTC，可选）",
            example = "2026-04-30T23:59:59Z")
    private Instant to;

    /**
     * Default constructor for Spring data binding.
     */
    public CollectionRunQueryRequest() {
        // Spring
    }

    public String getAdapterId() {
        return adapterId;
    }

    public void setAdapterId(final String v) {
        this.adapterId = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String v) {
        this.status = v;
    }

    public Instant getFrom() {
        return from;
    }

    public void setFrom(final Instant v) {
        this.from = v;
    }

    public Instant getTo() {
        return to;
    }

    public void setTo(final Instant v) {
        this.to = v;
    }
}

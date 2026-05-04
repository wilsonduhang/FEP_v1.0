package com.puchain.fep.web.collector.dto;

import com.puchain.fep.collector.support.CollectionRunResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * 手动触发数据采集运行的响应 DTO（PRD §2.2.3 + §5.5 报送管理）。
 *
 * <p>P4 T6b — {@code POST /api/v1/collector/triggers} 返回体。本 DTO 是
 * {@link com.puchain.fep.collector.support.CollectionRunResult} 的 Web 投影：
 * 暴露 {@code runId / adapterId / status / counts / errorMessage} 给管理 UI，
 * 与 {@link CollectionRunResponse}（GET 列表 DTO）字段一致以便前端复用渲染。</p>
 *
 * <p><b>SKIPPED 语义</b>：当分布式锁忙时 {@code status="SKIPPED"} + {@code runId=null}。
 * 这是正常业务结果（HTTP 200），不是错误。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Schema(description = "手动触发数据采集响应", name = "CollectorTriggerResponse")
public final class CollectorTriggerResponse {

    @Schema(description = "运行 ID（32 位 hex；SKIPPED 时为 null）", example = "abc123def456...")
    private final String runId;

    @Schema(description = "适配器 ID", example = "ADP_3101")
    private final String adapterId;

    @Schema(description = "运行状态：SUCCESS / PARTIAL / FAILED / SKIPPED",
            example = "SUCCESS")
    private final String status;

    @Schema(description = "已组装记录数", example = "10")
    private final int assembledCount;

    @Schema(description = "已入队记录数", example = "10")
    private final int submittedCount;

    @Schema(description = "失败记录数", example = "0")
    private final int errorCount;

    @Schema(description = "首个错误消息（无错误时 null；最长 1024 字符）",
            example = "null")
    private final String errorMessage;

    /**
     * Constructs a response from canonical fields.
     *
     * @param runId          run id (nullable for SKIPPED)
     * @param adapterId      adapter id (non-null)
     * @param status         status enum name (non-null)
     * @param assembledCount assembled count
     * @param submittedCount submitted count
     * @param errorCount     error count
     * @param errorMessage   first error message, nullable
     */
    public CollectorTriggerResponse(final String runId,
                                    final String adapterId,
                                    final String status,
                                    final int assembledCount,
                                    final int submittedCount,
                                    final int errorCount,
                                    final String errorMessage) {
        this.runId = runId;
        this.adapterId = Objects.requireNonNull(adapterId, "adapterId");
        this.status = Objects.requireNonNull(status, "status");
        this.assembledCount = assembledCount;
        this.submittedCount = submittedCount;
        this.errorCount = errorCount;
        this.errorMessage = errorMessage;
    }

    /**
     * Lifts a {@link CollectionRunResult} into a Web response DTO.
     *
     * @param r scheduler result, non-null
     * @return response dto
     */
    public static CollectorTriggerResponse from(final CollectionRunResult r) {
        Objects.requireNonNull(r, "result");
        return new CollectorTriggerResponse(
                r.runId(),
                r.adapterId(),
                r.status().name(),
                r.assembled(),
                r.submitted(),
                r.errors(),
                r.errorMessage());
    }

    public String getRunId() {
        return runId;
    }

    public String getAdapterId() {
        return adapterId;
    }

    public String getStatus() {
        return status;
    }

    public int getAssembledCount() {
        return assembledCount;
    }

    public int getSubmittedCount() {
        return submittedCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}

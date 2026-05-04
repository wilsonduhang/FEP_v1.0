package com.puchain.fep.web.collector.dto;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.collector.CollectionRunEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Objects;

/**
 * 采集运行历史响应 DTO（PRD §5.5 报送管理）。
 *
 * <p>P4 T6b — {@code GET /api/v1/collector/runs} 列表项投影。从
 * {@link CollectionRunEntity} 直接映射；{@code errorMessage} 在列表中保留以便
 * UI 显示运行状态摘要（V19 列已截断至 1024 字符，不存在大字段问题）。</p>
 *
 * <p><b>errorMessage CRLF 防御（T6b-fix MINOR #4）</b>：列表 DTO 暴露给管理 UI
 * 是设计选择（admin 调试需要看 SQLException stack trace 片段）。但 errorMessage
 * 源自 adapter / assembler 异常 message，不可信内容。{@link #from} 工厂调
 * {@link LogSanitizer#sanitize} 清理 CR/LF（防 UI 渲染时的换行注入 / log forging via UI），
 * 长度由 scheduler 端 {@code ERROR_MESSAGE_MAX_LENGTH=1024} 已截断不需重复处理。
 * Vue 前端的 HTML escape 防 XSS 是另一层防御（在前端层）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Schema(description = "采集运行历史响应", name = "CollectionRunResponse")
public final class CollectionRunResponse {

    @Schema(description = "运行 ID", example = "abc123def456...")
    private final String runId;

    @Schema(description = "适配器 ID", example = "ADP_3101")
    private final String adapterId;

    @Schema(description = "运行状态", example = "SUCCESS")
    private final String status;

    @Schema(description = "触发来源：SCHEDULED / MANUAL", example = "MANUAL")
    private final String triggerSource;

    @Schema(description = "开始时间（UTC）", example = "2026-04-30T10:00:00Z")
    private final Instant startedAt;

    @Schema(description = "结束时间（UTC；运行中为 null）", example = "2026-04-30T10:00:01Z")
    private final Instant completedAt;

    @Schema(description = "采集记录条数", example = "10")
    private final int collectedCount;

    @Schema(description = "已组装条数", example = "10")
    private final int assembledCount;

    @Schema(description = "已入队条数", example = "10")
    private final int submittedCount;

    @Schema(description = "失败条数", example = "0")
    private final int errorCount;

    @Schema(description = "首个错误消息（最长 1024 字符；无错误时 null）")
    private final String errorMessage;

    /**
     * Constructs a response from canonical fields.
     *
     * @param runId          run id (non-null)
     * @param adapterId      adapter id (non-null)
     * @param status         status name (non-null)
     * @param triggerSource  trigger source name (non-null)
     * @param startedAt      start instant (non-null)
     * @param completedAt    completion instant, nullable
     * @param collectedCount collected count
     * @param assembledCount assembled count
     * @param submittedCount submitted count
     * @param errorCount     error count
     * @param errorMessage   first error message, nullable
     */
    public CollectionRunResponse(final String runId,
                                 final String adapterId,
                                 final String status,
                                 final String triggerSource,
                                 final Instant startedAt,
                                 final Instant completedAt,
                                 final int collectedCount,
                                 final int assembledCount,
                                 final int submittedCount,
                                 final int errorCount,
                                 final String errorMessage) {
        this.runId = Objects.requireNonNull(runId, "runId");
        this.adapterId = Objects.requireNonNull(adapterId, "adapterId");
        this.status = Objects.requireNonNull(status, "status");
        this.triggerSource = Objects.requireNonNull(triggerSource, "triggerSource");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.completedAt = completedAt;
        this.collectedCount = collectedCount;
        this.assembledCount = assembledCount;
        this.submittedCount = submittedCount;
        this.errorCount = errorCount;
        this.errorMessage = errorMessage;
    }

    /**
     * Lifts a {@link CollectionRunEntity} into a response DTO.
     *
     * @param e entity, non-null
     * @return response dto
     */
    public static CollectionRunResponse from(final CollectionRunEntity e) {
        Objects.requireNonNull(e, "entity");
        // T6b-fix MINOR #4: CRLF defense on errorMessage before exposing to
        // management UI. LogSanitizer.sanitize returns null for null input,
        // preserving the "no error" semantics; otherwise replaces CR/LF with
        // spaces to neutralize newline injection in UI rendering.
        return new CollectionRunResponse(
                e.getRunId(),
                e.getAdapterId(),
                e.getStatus(),
                e.getTriggerSource(),
                e.getStartedAt(),
                e.getCompletedAt(),
                e.getCollectedCount(),
                e.getAssembledCount(),
                e.getSubmittedCount(),
                e.getErrorCount(),
                e.getErrorMessage() == null ? null : LogSanitizer.sanitize(e.getErrorMessage()));
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

    public String getTriggerSource() {
        return triggerSource;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public int getCollectedCount() {
        return collectedCount;
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

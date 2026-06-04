package com.puchain.fep.web.callback.dlq.dto;

import com.puchain.fep.web.callback.domain.CallbackQueueEntity;

import java.time.LocalDateTime;

/**
 * 死信队列条目响应 DTO（管理 Web 只读视图）。
 *
 * <p>暴露 DLQ 审计所需的元数据：状态 / 重试计数 / 末次错误 / 重放回溯链字段
 * （{@code originalDlqId} / {@code replayedBy} / {@code replayedAt}）。绝不暴露 payloadJson
 * 业务报文体（可能含敏感字段，审计查看走专用脱敏视图）。参见 PRD v1.3 §5.5.3
 * 回调可靠性（FR-INFRA-CALLBACK-DLQ-REPLAY）。</p>
 *
 * @param queueId           队列行 id
 * @param targetInterfaceId 目标行内接口 id
 * @param msgNo             inbound 报文号
 * @param status            队列状态（DLQ 列表恒为 {@code DEAD_LETTER}；重放链可含 PENDING 等）
 * @param retryCount        重试计数
 * @param lastError         末次错误摘要（≤500，已截断）
 * @param updateTime        末次更新时间
 * @param originalDlqId     源死信行 id（重放衍生行非空，构成审计回溯链）
 * @param replayedBy        触发重放的 admin 用户 id（重放衍生行非空）
 * @param replayedAt        重放触发时间（重放衍生行非空）
 * @author FEP Team
 * @since 1.0.0
 */
public record DlqEntryResponse(String queueId, String targetInterfaceId, String msgNo,
        String status, int retryCount, String lastError, LocalDateTime updateTime,
        String originalDlqId, String replayedBy, LocalDateTime replayedAt) {

    /**
     * 由队列实体投影为只读响应（不含 payloadJson）。
     *
     * @param e 队列实体，非空
     * @return 响应 DTO
     */
    public static DlqEntryResponse from(final CallbackQueueEntity e) {
        return new DlqEntryResponse(e.getQueueId(), e.getTargetInterfaceId(), e.getMsgNo(),
                e.getStatus(), e.getRetryCount(), e.getLastError(), e.getUpdateTime(),
                e.getOriginalDlqId(), e.getReplayedBy(), e.getReplayedAt());
    }
}

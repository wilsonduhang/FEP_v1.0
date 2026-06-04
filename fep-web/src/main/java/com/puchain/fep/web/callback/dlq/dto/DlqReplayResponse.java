package com.puchain.fep.web.callback.dlq.dto;

import java.time.LocalDateTime;

/**
 * 死信复制重放响应 DTO。
 *
 * <p>返回新建 {@code PENDING} 重放行 id + 源死信行 id + 重放触发时间，供管理 Web
 * 展示重放结果并跳转新行。原死信行保留作审计证据（不变更状态）。参见 PRD v1.3
 * §5.5.3 回调可靠性（FR-INFRA-CALLBACK-DLQ-REPLAY）。</p>
 *
 * @param newQueueId   新建的重放行 id（状态 {@code PENDING}，retryCount=0）
 * @param originalDlqId 源死信行 id（保留作审计证据）
 * @param replayedAt   重放触发时间
 * @author FEP Team
 * @since 1.0.0
 */
public record DlqReplayResponse(String newQueueId, String originalDlqId,
        LocalDateTime replayedAt) {
}

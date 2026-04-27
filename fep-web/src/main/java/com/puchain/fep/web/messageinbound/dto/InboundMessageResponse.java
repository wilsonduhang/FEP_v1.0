package com.puchain.fep.web.messageinbound.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 入站报文 REST 响应 DTO（PRD §5.3.2.13）。
 *
 * <p>P3 Task 2 — {@code POST /api/v1/messages/inbound} 的成功响应载荷。
 * 由 {@code InboundMessageDispatcher} 在调用 {@code SyncMessageProcessorService.processInbound}
 * 后构造，承载 3 个核心结果字段：</p>
 * <ul>
 *   <li>{@code recordId} — 持久化后的 {@code message_process_record.id}（32 位 UUID）。</li>
 *   <li>{@code status} — {@code MessageProcessStatus} 枚举名（{@code COMPLETED} / {@code FAILED} 等）。</li>
 *   <li>{@code eventPublished} — 是否发布了 {@code InboundMessageProcessedEvent}（仅当 status==COMPLETED 时为 true）。</li>
 * </ul>
 *
 * @param recordId       message_process_record.id（32 位 UUID），非空
 * @param status         {@code MessageProcessStatus} 枚举名，非空
 * @param eventPublished 是否发布了 {@code InboundMessageProcessedEvent}
 * @author FEP Team
 * @since 1.0.0
 */
@Schema(description = "入站报文响应", name = "InboundMessageResponse")
public record InboundMessageResponse(
        @Schema(description = "处理记录 ID（32 位 UUID）", example = "abcdef0123456789abcdef0123456789")
        String recordId,
        @Schema(description = "处理状态", example = "COMPLETED")
        String status,
        @Schema(description = "是否发布 InboundMessageProcessedEvent", example = "true")
        boolean eventPublished) {
}

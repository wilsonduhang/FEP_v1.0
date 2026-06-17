package com.puchain.fep.web.audit.review.dto;

/**
 * 审核决策请求体（PRD v1.3 §5.8）。
 *
 * <p>approve：{@code comment} 为审核意见（可空）；reject：{@code comment} 为驳回原因（必填，
 * 空白由服务层校验抛 {@code IllegalArgumentException} → HTTP 400）。</p>
 *
 * @param comment 审核意见 / 驳回原因
 * @author FEP Team
 * @since 1.0.0
 */
public record ReviewDecisionRequest(String comment) {
}

package com.puchain.fep.web.audit.review.dto;

import com.puchain.fep.web.audit.review.domain.MessageReviewTaskEntity;

/**
 * 审核任务响应 DTO（PRD v1.3 §5.8）。时间为 epoch millis（与持久层一致，前端按本地时区格式化）。
 *
 * @param reviewId         审核任务 id
 * @param messageRecordId  源报文处理记录 id
 * @param messageType      报文类型码
 * @param transitionNo     业务流水号
 * @param errorCode        失败错误码（PROC_8507）
 * @param violationSummary 首条违规文案（可空）
 * @param reviewStatus     审核状态（PENDING/APPROVED/REJECTED）
 * @param reviewLevel      需经过的总层级
 * @param currentLevel     当前待审层级
 * @param reviewerId       审核人 id（未审为 null）
 * @param reviewComment    审核意见 / 驳回原因（未审为 null）
 * @param createdAt        创建时间（epoch millis）
 * @param reviewedAt       审核完成时间（epoch millis，未审为 null）
 * @author FEP Team
 * @since 1.0.0
 */
public record ReviewTaskResponse(
        String reviewId,
        String messageRecordId,
        String messageType,
        String transitionNo,
        String errorCode,
        String violationSummary,
        String reviewStatus,
        int reviewLevel,
        int currentLevel,
        String reviewerId,
        String reviewComment,
        long createdAt,
        Long reviewedAt) {

    /**
     * 由实体构造响应 DTO。
     *
     * @param e 审核任务实体，非空
     * @return 响应 DTO
     */
    public static ReviewTaskResponse from(final MessageReviewTaskEntity e) {
        return new ReviewTaskResponse(
                e.getReviewId(), e.getMessageRecordId(), e.getMessageType(),
                e.getTransitionNo(), e.getErrorCode(), e.getViolationSummary(),
                e.getReviewStatus(), e.getReviewLevel(), e.getCurrentLevel(),
                e.getReviewerId(), e.getReviewComment(), e.getCreatedAt(), e.getReviewedAt());
    }
}

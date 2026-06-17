package com.puchain.fep.web.audit.review.service;

import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.audit.review.config.ReviewWorkflowProperties;
import com.puchain.fep.web.audit.review.domain.MessageReviewTaskEntity;
import com.puchain.fep.web.audit.review.domain.ReviewStatus;
import com.puchain.fep.web.audit.review.repository.MessageReviewTaskRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 报文人工审核任务服务（PRD v1.3 §5.8 多级审核 Phase2）。
 *
 * <p>本 Task 实现「业务规则失败报文 → 创建审核任务」（{@link #createFromFailedRecord}）；
 * 审核决策（列表/通过/驳回）在后续 Task 扩展。</p>
 *
 * <p>创建入口由 {@code JpaMessageProcessStore.updateStatus} 旁路调用（报文落
 * {@code FAILED + PROC_8507} 时），运行在该方法的事务内，与 FAILED 落库同事务保证原子性。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class MessageReviewTaskService {

    private static final Logger log = LoggerFactory.getLogger(MessageReviewTaskService.class);

    private final MessageReviewTaskRepository repository;
    private final ReviewWorkflowProperties properties;

    /**
     * 构造注入仓储与工作流配置。
     *
     * @param repository 审核任务仓储，非空
     * @param properties 审核工作流配置（层级数），非空
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-injected singleton config bean (ReviewWorkflowProperties); "
                    + "shared reference is intentional DI, not external mutable state")
    public MessageReviewTaskService(final MessageReviewTaskRepository repository,
                                    final ReviewWorkflowProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /**
     * 为一条业务规则失败（{@code PROC_8507}）的报文创建待审核任务。
     *
     * <p>幂等：同一 {@code messageRecordId} 已存在审核任务时直接返回（不重复创建）。
     * 数据库 {@code uq_msg_review_record} 唯一约束为并发后盾。</p>
     *
     * @param messageRecordId  源 {@code message_process_record.id}，非空
     * @param messageType      报文类型码（如 {@code 1001}），非空
     * @param transitionNo     业务流水号，非空
     * @param errorCode        失败错误码（{@code PROC_8507}），非空
     * @param violationSummary 首条违规文案（可为 null）
     */
    @Transactional
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "messageRecordId/messageType passed through LogSanitizer.sanitize() prior to LOG")
    public void createFromFailedRecord(final String messageRecordId,
                                       final String messageType,
                                       final String transitionNo,
                                       final String errorCode,
                                       final String violationSummary) {
        if (repository.findByMessageRecordId(messageRecordId).isPresent()) {
            log.debug("review task already exists for messageRecordId={}, skip",
                    LogSanitizer.sanitize(messageRecordId));
            return;
        }
        final MessageReviewTaskEntity task = new MessageReviewTaskEntity();
        task.setReviewId(IdGenerator.uuid32());
        task.setMessageRecordId(messageRecordId);
        task.setMessageType(messageType);
        task.setTransitionNo(transitionNo);
        task.setErrorCode(errorCode);
        task.setViolationSummary(violationSummary);
        task.setReviewStatus(ReviewStatus.PENDING.name());
        task.setReviewLevel(properties.getLevels());
        task.setCurrentLevel(1);
        task.setCreatedAt(Instant.now().toEpochMilli());
        repository.save(task);
        log.info("review task created messageRecordId={} msgType={} levels={}",
                LogSanitizer.sanitize(messageRecordId),
                LogSanitizer.sanitize(messageType),
                properties.getLevels());
    }
}

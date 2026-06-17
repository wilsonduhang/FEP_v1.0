package com.puchain.fep.web.audit.review.service;

import com.puchain.fep.common.domain.PageResult;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.audit.review.config.ReviewWorkflowProperties;
import com.puchain.fep.web.audit.review.domain.MessageReviewTaskEntity;
import com.puchain.fep.web.audit.review.domain.ReviewStatus;
import com.puchain.fep.web.audit.review.dto.ReviewTaskResponse;
import com.puchain.fep.web.audit.review.repository.MessageReviewTaskRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
     * <p><strong>事务隔离（best-effort，muzhou 2026-06-17 决策）</strong>：本方法以
     * {@link Propagation#REQUIRES_NEW} 在<strong>独立事务</strong>中创建审核任务——审核任务创建
     * 失败（极罕见并发唯一冲突）不污染调用方（{@code JpaMessageProcessStore.updateStatus}）的
     * 报文 FAILED 落库事务，保证报文必达终态（中转 liveness 不变量优先于审核完整性；
     * 丢失的审核任务可由 FAILED+PROC_8507 记录扫描回填）。</p>
     *
     * @param messageRecordId  源 {@code message_process_record.id}，非空
     * @param messageType      报文类型码（如 {@code 1001}），非空
     * @param transitionNo     业务流水号，非空
     * @param errorCode        失败错误码（{@code PROC_8507}），非空
     * @param violationSummary 首条违规文案（可为 null）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

    /**
     * 分页查询审核任务，按创建时间倒序。
     *
     * @param status   审核状态过滤；{@code null} 表示全部
     * @param pageNum  页码（1-based，&lt;1 归一为 1）
     * @param pageSize 每页条数
     * @return 分页结果（1-based 回显）
     */
    @Transactional(readOnly = true)
    public PageResult<ReviewTaskResponse> list(final ReviewStatus status,
                                               final int pageNum,
                                               final int pageSize) {
        final int safePage = Math.max(pageNum, 1);
        final Pageable pageable = PageRequest.of(safePage - 1, pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        final Page<MessageReviewTaskEntity> page = status == null
                ? repository.findAll(pageable)
                : repository.findByReviewStatus(status.name(), pageable);
        final List<ReviewTaskResponse> records = page.getContent().stream()
                .map(ReviewTaskResponse::from)
                .toList();
        return new PageResult<>(records, page.getTotalElements(), safePage, pageSize);
    }

    /**
     * 按 id 查询审核任务详情。
     *
     * @param reviewId 审核任务 id
     * @return 响应 DTO
     * @throws NoSuchElementException id 不存在
     */
    @Transactional(readOnly = true)
    public ReviewTaskResponse getById(final String reviewId) {
        return repository.findById(reviewId)
                .map(ReviewTaskResponse::from)
                .orElseThrow(() -> new NoSuchElementException("review task not found: " + reviewId));
    }

    /**
     * 审核通过。单级（{@code levels=1}）一次通过即终结为 {@link ReviewStatus#APPROVED}；
     * 多级（{@code levels>1}）逐级推进 {@code currentLevel}，达到 {@code reviewLevel} 才终结。
     *
     * @param reviewId   审核任务 id
     * @param reviewerId 审核人 id，非空
     * @param comment    审核意见（可空）
     * @throws NoSuchElementException id 不存在
     * @throws IllegalStateException  任务已是终态（APPROVED/REJECTED）
     */
    @Transactional
    public void approve(final String reviewId, final String reviewerId, final String comment) {
        final MessageReviewTaskEntity t = loadPending(reviewId);
        if (t.getCurrentLevel() >= t.getReviewLevel()) {
            t.setReviewStatus(ReviewStatus.APPROVED.name());
            t.setReviewerId(reviewerId);
            t.setReviewComment(comment);
            t.setReviewedAt(Instant.now().toEpochMilli());
        } else {
            t.setCurrentLevel(t.getCurrentLevel() + 1);
            t.setReviewerId(reviewerId);
        }
        repository.save(t);
    }

    /**
     * 审核驳回。任一层驳回即终结为 {@link ReviewStatus#REJECTED}。
     *
     * @param reviewId   审核任务 id
     * @param reviewerId 审核人 id，非空
     * @param reason     驳回原因，非空白
     * @throws NoSuchElementException   id 不存在
     * @throws IllegalStateException    任务已是终态
     * @throws IllegalArgumentException {@code reason} 为空白
     */
    @Transactional
    public void reject(final String reviewId, final String reviewerId, final String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reject reason must not be blank");
        }
        final MessageReviewTaskEntity t = loadPending(reviewId);
        t.setReviewStatus(ReviewStatus.REJECTED.name());
        t.setReviewerId(reviewerId);
        t.setReviewComment(reason);
        t.setReviewedAt(Instant.now().toEpochMilli());
        repository.save(t);
    }

    private MessageReviewTaskEntity loadPending(final String reviewId) {
        final MessageReviewTaskEntity t = repository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("review task not found: " + reviewId));
        if (!ReviewStatus.PENDING.name().equals(t.getReviewStatus())) {
            throw new IllegalStateException(
                    "review task already terminal: " + t.getReviewStatus());
        }
        return t;
    }
}

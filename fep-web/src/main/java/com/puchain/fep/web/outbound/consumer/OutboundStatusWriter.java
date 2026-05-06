package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.web.outbound.OutboundMessageQueueEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

/**
 * P5 B1 — outbound queue 状态机回写 @Transactional 边界。
 *
 * <p>从 {@link OutboundQueueRunnerImpl} 拆出，承担两类原子状态变更：</p>
 * <ul>
 *   <li>{@link #recordSent(String, String, String, Instant)} — 成功 → status='SENT'</li>
 *   <li>{@link #recordFailure(String, Throwable)} — 失败 → 委派 {@link OutboundRetryHandler}
 *       内部决定 RETRY / DEAD_LETTER 状态机转移</li>
 * </ul>
 *
 * <p><b>Tx 边界</b>：本 @Service 上的方法每个独立开启 PROPAGATION_REQUIRED Tx
 * （@Transactional 默认 propagation=REQUIRED，未显式声明）；
 * caller (RunnerImpl) 不持有 Tx，确保 TLQ 网络 IO 在 Tx 之外执行（B1 设计目标）。</p>
 *
 * <p><b>Spring 代理约束</b>：{@code @Transactional} 必须由外部 Bean 调用方能触发 AOP 代理；
 * 严禁在同类内通过 {@code this.recordSent(...)} 自调用——故拆为独立 @Service。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class OutboundStatusWriter {

    private final OutboundQueueRepository repository;
    private final OutboundRetryHandler retryHandler;

    /**
     * Spring 构造器注入。
     *
     * @param repository   outbound queue JPA repository（非空）
     * @param retryHandler 失败路径重试 / DLQ 处理器（非空）
     */
    public OutboundStatusWriter(final OutboundQueueRepository repository,
                                final OutboundRetryHandler retryHandler) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.retryHandler = Objects.requireNonNull(retryHandler, "retryHandler");
    }

    /**
     * 成功路径：写 status='SENT' + msgId + sentAt + tlqSendResult + updatedAt 五字段。
     *
     * @param queueId       queue 主键（非空）
     * @param msgId         TLQ 返回 msgId（非空）
     * @param tlqSendResult TLQ 返回 result 字段（非空）
     * @param sentAt        发送时刻（非空，updatedAt 取同值保持原 RunnerImpl 行为）
     * @throws IllegalStateException queueId 在数据库中不存在
     */
    @Transactional
    public void recordSent(final String queueId,
                           final String msgId,
                           final String tlqSendResult,
                           final Instant sentAt) {
        Objects.requireNonNull(queueId, "queueId");
        Objects.requireNonNull(msgId, "msgId");
        Objects.requireNonNull(tlqSendResult, "tlqSendResult");
        Objects.requireNonNull(sentAt, "sentAt");

        final OutboundMessageQueueEntity entity = repository.findById(queueId)
            .orElseThrow(() -> new IllegalStateException("queue_id not found: " + queueId));
        entity.setStatus("SENT");
        entity.setMsgId(msgId);
        entity.setSentAt(sentAt);
        entity.setTlqSendResult(tlqSendResult);
        entity.setUpdatedAt(sentAt);
        repository.save(entity);
    }

    /**
     * 失败路径：委派 {@link OutboundRetryHandler#handleFailure}，由其决定
     * RETRY（retry_count++ / next_retry_at）或 DEAD_LETTER 转移。
     *
     * <p>RetryHandler 类层无 @Transactional——本方法的 Tx 边界覆盖 RetryHandler.repo.save()。</p>
     *
     * @param queueId queue 主键
     * @param error   触发失败的 throwable（透传给 RetryHandler 写 error_message）
     * @throws IllegalStateException queueId 不存在（RetryHandler 内部抛出）
     */
    @Transactional
    public void recordFailure(final String queueId, final Throwable error) {
        Objects.requireNonNull(queueId, "queueId");
        retryHandler.handleFailure(queueId, error);
    }
}

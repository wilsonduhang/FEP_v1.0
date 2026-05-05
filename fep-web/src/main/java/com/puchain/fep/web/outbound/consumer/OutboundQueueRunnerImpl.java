package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import com.puchain.fep.web.outbound.OutboundMessageQueueEntity;
import com.puchain.fep.web.outbound.consumer.OutboundTlqSender.OutboundSendOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

/**
 * P5 T9 outbound queue runner 生产实现：串联 T4 EnvelopeBuilder + T5 SignAdapter + T6 TlqSender
 * + T7 RetryHandler + T8 Metrics 完成单条 queue_id 的 read → build → sign → send → 状态回写流水。
 *
 * <p>状态机转移（成功路径）：</p>
 * <ol>
 *   <li>{@link OutboundQueueRepository#findById} 加载 entity（不存在抛
 *       {@link IllegalStateException}，与 {@link OutboundRetryHandler} 同模式）</li>
 *   <li>{@link OutboundHeadXmlParser#parse} 反序列化 {@code message_head_xml}</li>
 *   <li>{@link OutboundCfxEnvelopeBuilder#build} 组装 CFX envelope</li>
 *   <li>{@link OutboundSignAdapter#embedSignatureAsComment} 嵌入 SM2 签名注释</li>
 *   <li>{@link OutboundTlqSender#send} 推送 TLQ BATCH_SEND 通道</li>
 *   <li>outcome.success() == true → status='SENT' + msg_id + sent_at + tlq_send_result + updated_at</li>
 *   <li>{@link OutboundMetrics#recordSent} 记录延迟（System.nanoTime 差值）</li>
 * </ol>
 *
 * <p>状态机转移（失败路径）：outcome.success() == false 或抛 {@link Throwable} →
 * delegate {@link OutboundRetryHandler#handleFailure}（内部处理 retry++ / RETRY / DEAD_LETTER），
 * 然后 re-read 新 status 决定 metrics 分支（recordRetry / recordDeadLetter）。</p>
 *
 * <p><b>per-row 隔离</b>：单条失败仅当前事务回滚到 retry 状态，不影响 batch 内其它行
 * （{@link OutboundQueueConsumer#poll()} 逐行调用本方法，外层 try/catch 兜底）。</p>
 *
 * <p><b>@Transactional</b>：本方法 read-write 都走同一事务，确保状态字段原子更新。
 * RetryHandler 内部 save 也在同事务内。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class OutboundQueueRunnerImpl implements OutboundQueueRunner {

    private static final Logger LOG = LoggerFactory.getLogger(OutboundQueueRunnerImpl.class);

    private final OutboundQueueRepository repository;
    private final OutboundCfxEnvelopeBuilder envelopeBuilder;
    private final OutboundSignAdapter signAdapter;
    private final OutboundTlqSender tlqSender;
    private final OutboundRetryHandler retryHandler;
    private final OutboundMetrics metrics;

    /**
     * 构造注入 6 项依赖。
     *
     * @param repository      outbound queue Repository（findById / save）
     * @param envelopeBuilder CFX envelope 组装器（T4）
     * @param signAdapter     SM2 签名注释嵌入（T5）
     * @param tlqSender       TLQ BATCH_SEND 推送（T6）
     * @param retryHandler    失败 retry / DLQ 状态机（T7）
     * @param metrics         Counter / Timer telemetry（T8）
     */
    public OutboundQueueRunnerImpl(
            final OutboundQueueRepository repository,
            final OutboundCfxEnvelopeBuilder envelopeBuilder,
            final OutboundSignAdapter signAdapter,
            final OutboundTlqSender tlqSender,
            final OutboundRetryHandler retryHandler,
            final OutboundMetrics metrics) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.envelopeBuilder = Objects.requireNonNull(envelopeBuilder, "envelopeBuilder");
        this.signAdapter = Objects.requireNonNull(signAdapter, "signAdapter");
        this.tlqSender = Objects.requireNonNull(tlqSender, "tlqSender");
        this.retryHandler = Objects.requireNonNull(retryHandler, "retryHandler");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
    }

    /**
     * 执行单条 queue 行的发送流水。
     *
     * <p>外层 try/catch 兜底任意异常（envelope build / sign / send）→ 转 retry 处理；
     * RetryHandler 自身已 swallow 异常（仅打 WARN 日志），不会再向上抛。</p>
     *
     * @param queueId 已被 claimBatch 持锁的 queue_id
     */
    @Override
    @Transactional
    public void run(final String queueId) {
        Objects.requireNonNull(queueId, "queueId");
        final OutboundMessageQueueEntity entity = repository.findById(queueId)
                .orElseThrow(() -> new IllegalStateException("queue_id not found: " + queueId));

        final long t0 = System.nanoTime();
        try {
            final OutboundHeadFields headFields = OutboundHeadXmlParser.parse(entity.getMessageHeadXml());
            final String envelope = envelopeBuilder.build(entity, headFields);
            final String signedXml = signAdapter.embedSignatureAsComment(envelope);
            final OutboundSendOutcome outcome = tlqSender.send(signedXml);

            if (outcome.success()) {
                final Instant now = Instant.now();
                entity.setStatus("SENT");
                entity.setMsgId(outcome.msgId());
                entity.setSentAt(now);
                entity.setTlqSendResult(outcome.tlqSendResult());
                entity.setUpdatedAt(now);
                repository.save(entity);
                metrics.recordSent(System.nanoTime() - t0);
                LOG.info("Outbound SENT: queue_id={} msg_id={}", queueId, outcome.msgId());
            } else {
                handleSendFailure(queueId, new FepBusinessException(
                        FepErrorCode.OUTBOUND_5104_SEND_FAILURE,
                        "TLQ send returned failure: " + outcome.tlqSendResult()));
            }
        } catch (RuntimeException e) {
            handleSendFailure(queueId, e);
        }
    }

    /**
     * 失败兜底：委派 {@link OutboundRetryHandler#handleFailure}，然后 re-read entity
     * 决定 metrics 分支（RETRY / DEAD_LETTER）。
     *
     * <p>RetryHandler swallow 异常（不抛 RuntimeException），所以这里不需再 try/catch。
     * Re-read 用 repository.findById 而非缓存 entity，因为 RetryHandler 内部已 mutate
     * 了 status / retry_count 并 save，read-back 拿到新 status。</p>
     *
     * @param queueId queue id
     * @param error   触发失败的 throwable（透传给 RetryHandler 写 error_message）
     */
    private void handleSendFailure(final String queueId, final Throwable error) {
        // Include cause chain in log to make root cause visible — a wrapped FepBusinessException
        // hides the JAXB / reflection root cause from operator without stack trace.
        LOG.warn("Outbound send failure for queue_id={}", queueId, error);
        retryHandler.handleFailure(queueId, error);
        // re-read 决定 metrics 分支
        repository.findById(queueId).ifPresent(updated -> {
            if ("DEAD_LETTER".equals(updated.getStatus())) {
                metrics.recordDeadLetter();
            } else if ("RETRY".equals(updated.getStatus())) {
                metrics.recordRetry();
            }
        });
    }
}

package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.LogSanitizer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import com.puchain.fep.web.outbound.OutboundMessageQueueEntity;
import com.puchain.fep.web.outbound.consumer.OutboundTlqSender.OutboundSendOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

/**
 * P5 T9 outbound queue runner 生产实现：串联 T4 EnvelopeBuilder + T5 SignAdapter + T6 TlqSender
 * + T8 Metrics 完成单条 queue_id 的 read → build → sign → send → 状态回写流水。
 *
 * <p>状态机转移（成功路径）：</p>
 * <ol>
 *   <li>{@link OutboundQueueRepository#findById} 加载 entity（不存在抛
 *       {@link IllegalStateException}，与 {@link OutboundRetryHandler} 同模式）</li>
 *   <li>{@link OutboundHeadXmlParser#parse} 反序列化 {@code message_head_xml}</li>
 *   <li>{@link OutboundCfxEnvelopeBuilder#build} 组装 CFX envelope</li>
 *   <li>{@link OutboundSignAdapter#embedSignatureAsComment} 嵌入 SM2 签名注释</li>
 *   <li>{@link OutboundTlqSender#send} 推送 TLQ BATCH_SEND 通道</li>
 *   <li>outcome.success() == true → 委派 {@link OutboundStatusWriterService#recordSent} 写入
 *       status='SENT' + msg_id + sent_at + tlq_send_result + updated_at（独立 @Transactional）</li>
 *   <li>{@link OutboundMetrics#recordSent} 记录延迟（System.nanoTime 差值）</li>
 * </ol>
 *
 * <p>状态机转移（失败路径）：outcome.success() == false 或抛 {@link Throwable} →
 * 委派 {@link OutboundStatusWriterService#recordFailure}（其内部 @Transactional Tx 内调用
 * {@link OutboundRetryHandler#handleFailure} 处理 retry++ / RETRY / DEAD_LETTER），
 * 然后 re-read 新 status 决定 metrics 分支（recordRetry / recordDeadLetter）。</p>
 *
 * <p><b>per-row 隔离</b>：单条失败仅当前 StatusWriter 内的 Tx 回滚到 retry 状态，不影响 batch
 * 内其它行（{@link OutboundQueueConsumer#poll()} 逐行调用本方法，外层 try/catch 兜底）。</p>
 *
 * <p><b>B1 后无 @Transactional</b>：本方法运行在非事务上下文，TLQ 网络 IO 不阻塞 DB 连接池；
 * 状态机回写通过 {@link OutboundStatusWriterService} 的独立 @Transactional 方法承载（每次写入独立短 Tx）。</p>
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
    private final OutboundStatusWriterService statusWriter;
    private final OutboundMetrics metrics;

    /**
     * 构造注入 6 项依赖。
     *
     * @param repository      outbound queue Repository（findById；写入由 statusWriter 承担）
     * @param envelopeBuilder CFX envelope 组装器（T4）
     * @param signAdapter     SM2 签名注释嵌入（T5）
     * @param tlqSender       TLQ BATCH_SEND 推送（T6）
     * @param statusWriter    状态机回写 @Service（B1 拆出，独立 @Transactional 边界）
     * @param metrics         Counter / Timer telemetry（T8）
     */
    public OutboundQueueRunnerImpl(
            final OutboundQueueRepository repository,
            final OutboundCfxEnvelopeBuilder envelopeBuilder,
            final OutboundSignAdapter signAdapter,
            final OutboundTlqSender tlqSender,
            final OutboundStatusWriterService statusWriter,
            final OutboundMetrics metrics) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.envelopeBuilder = Objects.requireNonNull(envelopeBuilder, "envelopeBuilder");
        this.signAdapter = Objects.requireNonNull(signAdapter, "signAdapter");
        this.tlqSender = Objects.requireNonNull(tlqSender, "tlqSender");
        this.statusWriter = Objects.requireNonNull(statusWriter, "statusWriter");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
    }

    /**
     * 编排单条 queue 行的发送流水。
     *
     * <p><b>B1 后无 @Transactional</b>：本方法运行在非事务上下文，TLQ 网络 IO 不阻塞 DB 连接池；
     * 状态机回写通过 {@link OutboundStatusWriterService} 的独立 @Transactional 方法承载。</p>
     *
     * <p>初始 read 不显式开 Tx — repo.findById 由 Hibernate 自动 SELECT（H2/MySQL autocommit），
     * 不需要事务保护。entity 在 try 块内只读用于构 envelope，不会被本方法写回。</p>
     *
     * <p>外层 try/catch 兜底任意异常（envelope build / sign / send）→ 转 statusWriter.recordFailure
     * 走 retry / DLQ 状态机。catch Exception（不仅 RuntimeException）覆盖 SignService /
     * KeyService 等 security 接口未来如声明 checked exception 的迁移路径，避免逃逸到
     * {@link OutboundQueueConsumer#poll()} 外层。</p>
     *
     * @param queueId 已被 {@link OutboundQueueRepository#claimBatch(int)} 持锁的 queue_id
     */
    // queueId/msgId 来自 DB 主键 + LogSanitizer.sanitize 兜底；handleSendFailure caller chain 多 instance 由 byte-code 追踪误报
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "input from DB PK + LogSanitizer.sanitize wraps; "
                    + "SpotBugs find-sec-bugs cannot detect user-defined sanitizer")
    @Override
    public void run(final String queueId) {
        Objects.requireNonNull(queueId, "queueId");
        final OutboundMessageQueueEntity entity = repository.findById(queueId)
                .orElseThrow(() -> new IllegalStateException("queue_id not found: " + queueId));

        final long t0 = System.nanoTime();
        try {
            final OutboundHeadFields headFields = OutboundHeadXmlParser.parse(entity.getMessageHeadXml());
            final OutboundCfxEnvelopeBuilder.EnvelopeBuildResult built = envelopeBuilder.build(entity, headFields);
            final String signedXml = signAdapter.embedSignatureAsComment(built.envelope());
            final OutboundSendOutcome outcome = tlqSender.send(signedXml, built.msgId());

            if (outcome.success()) {
                final Instant now = Instant.now();
                statusWriter.recordSent(queueId, outcome.msgId(), outcome.tlqSendResult(), now);
                metrics.recordSent(System.nanoTime() - t0);
                LOG.info("Outbound SENT: queue_id={} msg_id={}",
                        LogSanitizer.sanitize(queueId), LogSanitizer.sanitize(outcome.msgId()));
            } else {
                handleSendFailure(queueId, new FepBusinessException(
                        FepErrorCode.OUTBOUND_5104_SEND_FAILURE,
                        "TLQ send returned failure: " + outcome.tlqSendResult()));
            }
        } catch (Exception e) {
            handleSendFailure(queueId, e);
        }
    }

    /**
     * 失败兜底：委派 {@link OutboundStatusWriterService#recordFailure}（其内部 @Transactional Tx 内调用
     * {@link OutboundRetryHandler#handleFailure}），然后 re-read entity 决定 metrics 分支
     * （RETRY / DEAD_LETTER）。
     *
     * <p>Re-read 用 repository.findById 而非缓存 entity，因为 StatusWriter 内部已 mutate
     * status/retry_count 并 save，read-back 拿到新 status。re-read 在 statusWriter Tx 提交后
     * 进行，避免读到未提交的中间态。</p>
     *
     * @param queueId queue id
     * @param error   触发失败的 throwable（透传给 StatusWriter 写 error_message）
     */
    // queueId 来自 DB 主键 + LogSanitizer.sanitize 兜底；error 由 SLF4J 内部处理不拼接 log message
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "input from DB PK + LogSanitizer.sanitize wraps; "
                    + "SpotBugs find-sec-bugs cannot detect user-defined sanitizer")
    private void handleSendFailure(final String queueId, final Throwable error) {
        // Include cause chain in log to make root cause visible — a wrapped FepBusinessException
        // hides the JAXB / reflection root cause from operator without stack trace.
        LOG.warn("Outbound send failure for queue_id={}", LogSanitizer.sanitize(queueId), error);
        statusWriter.recordFailure(queueId, error);
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

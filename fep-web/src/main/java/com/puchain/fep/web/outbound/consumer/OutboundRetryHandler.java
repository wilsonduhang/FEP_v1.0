package com.puchain.fep.web.outbound.consumer;

// v0.5 修订 M1: OutboundQueueRepository 与本类同在 com.puchain.fep.web.outbound.consumer
// 包内（见 T2 Step 3），同包类无需 import。
import com.puchain.fep.common.util.LogSanitizer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.puchain.fep.web.outbound.OutboundMessageQueueEntity;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * P5 T7 outbound 发送失败处理器：retry 计数累加 + 状态机转移 + DLQ 兜底。
 *
 * <p>由 {@link OutboundQueueRunner} 在 {@link OutboundTlqSender} 报告失败、
 * Builder/Sign 抛异常时调用：</p>
 * <ul>
 *   <li>{@code retry_count++}，{@code error_message} 截断到 1024 字符</li>
 *   <li>累计次数 ≥ {@link OutboundQueueProperties.Retry#maxAttempts()}（默认 5）
 *       → {@code status='DEAD_LETTER'} + {@code next_retry_at=null} + WARN 日志
 *       （不抛异常，避免中断同批其它行）</li>
 *   <li>否则 → {@code status='RETRY'} + {@code next_retry_at = NOW + exp_backoff}</li>
 * </ul>
 *
 * <p><b>exp_backoff 公式</b>（与 P4 collector {@code RetryProperties} 一致）：</p>
 * <pre>
 *   shift = min(newRetryCount, 30)                              // 防 long 左移溢出
 *   backoff = min(backoffMillis &lt;&lt; shift, maxBackoffMillis) // cap 在 30 分钟
 * </pre>
 *
 * <p>默认 {@code backoffMillis=30s}, {@code maxBackoffMillis=30min}。
 * cap 在 ~7 次重试后命中（30s &lt;&lt; 6 = 1920000 &gt; 1800000）。</p>
 *
 * <p><b>DLQ 不抛异常</b>：Plan §AC3 措辞为"警告日志"（非"抛异常"），与 batch 处理
 * 单行容错语义一致 — Runner 可继续处理批次内其它行。</p>
 *
 * <p><b>{@code next_retry_at=null} on DLQ</b>：避免被
 * {@link OutboundQueueRepository#claimBatch(int)} 的 {@code RETRY} 分支误捞
 * （{@code WHERE status='RETRY' AND next_retry_at &lt;= CURRENT_TIMESTAMP}）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class OutboundRetryHandler {

    /** Logger for retry / DLQ 转移 telemetry。 */
    private static final Logger LOG = LoggerFactory.getLogger(OutboundRetryHandler.class);

    /** {@code error_message} 列长度上限（V22 ship TEXT，应用层主动截断防 H2 边界差异）。 */
    private static final int ERROR_MESSAGE_MAX_LENGTH = 1024;

    /** 防 {@code long} 左移溢出的 shift 上限（30 已足够覆盖 30 分钟 cap 之内的所有可能值）。 */
    private static final int MAX_SHIFT_BITS = 30;

    private final OutboundQueueRepository repo;
    private final OutboundQueueProperties props;
    private final Clock clock;

    /**
     * 构造注入 3 项依赖。
     *
     * @param repo   outbound 队列 Repository（持锁声领同包，T2 ship）
     * @param props  retry 配置（默认 maxAttempts=5 / backoffMillis=30s / maxBackoffMillis=30min）
     * @param clock  时钟，用于计算 {@code next_retry_at}（测试可注 fixed clock）
     */
    public OutboundRetryHandler(final OutboundQueueRepository repo,
                                final OutboundQueueProperties props,
                                final Clock clock) {
        this.repo = Objects.requireNonNull(repo, "repo");
        this.props = Objects.requireNonNull(props, "props");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 处理 outbound 发送失败：累加重试计数并转移状态。
     *
     * <p>同步 save 一次。本方法不会抛 {@link RuntimeException} 中断 Runner 批处理，
     * 仅在累计 ≥ maxAttempts 时打 WARN 日志（DLQ 转移）。{@code queue_id} 找不到时
     * 抛 {@link IllegalStateException}（Runner 自身有上层 try-catch 兜底）。</p>
     *
     * @param queueId outbound_message_queue.queue_id（VARCHAR(32) UUID-no-dash）
     * @param error   Sender / Builder / Sign 抛出的异常（可为 {@link Throwable}）
     */
    // queueId 来自 DB 主键 + LogSanitizer.sanitize 兜底；newRetryCount 整型，无 CRLF 风险
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "queueId from DB PK + LogSanitizer.sanitize wraps")
    public void handleFailure(final String queueId, final Throwable error) {
        final OutboundMessageQueueEntity entity = repo.findById(queueId)
            .orElseThrow(() -> new IllegalStateException("queue_id not found: " + queueId));

        final int newRetryCount = entity.getRetryCount() + 1;
        entity.setRetryCount(newRetryCount);
        entity.setErrorMessage(truncate(
            error == null ? null : error.getMessage(),
            ERROR_MESSAGE_MAX_LENGTH));

        if (newRetryCount >= props.retry().maxAttempts()) {
            entity.setStatus("DEAD_LETTER");
            entity.setNextRetryAt(null); // DLQ 不再调度
            LOG.warn("queue_id={} -> DEAD_LETTER (retry_count={})",
                LogSanitizer.sanitize(queueId), newRetryCount);
        } else {
            entity.setStatus("RETRY");
            final long shift = Math.min(newRetryCount, MAX_SHIFT_BITS);
            final long backoff = Math.min(
                props.retry().backoffMillis() << shift,
                props.retry().maxBackoffMillis());
            entity.setNextRetryAt(Instant.now(clock).plusMillis(backoff));
        }
        repo.save(entity);
    }

    /**
     * 截断字符串到 max 长度。null-safe。
     *
     * @param s   原始字符串（可空）
     * @param max 长度上限（必 &gt; 0）
     * @return 不超过 max 的字符串；输入为 null 则返回 null
     */
    private String truncate(final String s, final int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}

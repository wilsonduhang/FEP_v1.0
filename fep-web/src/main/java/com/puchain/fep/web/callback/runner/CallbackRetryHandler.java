package com.puchain.fep.web.callback.runner;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.config.CallbackQueueProperties;
import com.puchain.fep.web.callback.dlq.event.CallbackDeadLetterEvent;
import com.puchain.fep.web.callback.domain.CallbackQueueEntity;
import com.puchain.fep.web.callback.http.CallbackResult;
import com.puchain.fep.web.callback.repository.CallbackQueueRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 回调投递失败处理器：4xx 失败分类 + 指数退避 + 死信兜底（镜像
 * {@code OutboundRetryHandler}，HTTP 语义特化）。
 *
 * <ul>
 *   <li>4xx（400-499，配置/契约错误）→ 直接 DEAD_LETTER（重试无益，促 admin 修配置）</li>
 *   <li>5xx / 超时 / IO（statusCode=0）→ retry_count++，累计 ≥ 有效 maxAttempts → DEAD_LETTER，
 *       否则 RETRY + {@code next_retry_at = now + min(base&lt;&lt;min(count,30), max)}</li>
 * </ul>
 *
 * <p>有效 maxAttempts = {@code interfaceRetryCount > 0 ? interfaceRetryCount
 * : props.retry().maxAttempts()}（per-interface 优先，回退全局默认 3，PRD §5.5.2）。</p>
 *
 * <p><b>exp_backoff 公式</b>（与 {@code OutboundRetryHandler} 一致）：</p>
 * <pre>
 *   shift = min(newRetryCount, 30)                              // 防 long 左移溢出
 *   backoffMs = min(backoffMillis &lt;&lt; shift, maxBackoffMillis) // cap 在 30 分钟
 * </pre>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class CallbackRetryHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackRetryHandler.class);

    /** 防 {@code long} 左移溢出的 shift 上限。 */
    private static final int MAX_SHIFT_BITS = 30;

    /** HTTP 4xx 范围下界（含）。 */
    private static final int HTTP_CLIENT_ERROR_MIN = 400;

    /** HTTP 4xx 范围上界（含）。 */
    private static final int HTTP_CLIENT_ERROR_MAX = 499;

    private final CallbackQueueRepository repo;
    private final CallbackQueueProperties props;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 构造注入 4 项依赖。
     *
     * @param repo           回调队列 Repository，非空
     * @param props          退避配置（base/max/maxAttempts 默认），非空
     * @param clock          全局唯一 Clock bean（测试可注 fixed clock），非空
     * @param eventPublisher Spring 事件发布器，DEAD_LETTER 时发布
     *                       {@link CallbackDeadLetterEvent}（T10 事件解耦），非空
     */
    public CallbackRetryHandler(final CallbackQueueRepository repo,
                                final CallbackQueueProperties props,
                                final Clock clock,
                                final ApplicationEventPublisher eventPublisher) {
        this.repo = Objects.requireNonNull(repo, "repo");
        this.props = Objects.requireNonNull(props, "props");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
    }

    /**
     * 处理一次投递失败：决定 RETRY 或 DEAD_LETTER 并持久化（save 一次）。
     *
     * <p>本方法不抛 {@link RuntimeException} 中断 runner 批处理；所有路径均完成 save。</p>
     *
     * @param entity              失败的队列条目（已声领 SENDING），非空
     * @param interfaceRetryCount {@code SubOutputInterface.retryCount}（&le;0 回退全局默认）
     * @param result              HTTP 推送结果（statusCode 用于 4xx 分类；IO 异常时为 0）
     * @return 终态 {@link CallbackFailureOutcome}，供 runner 记 metrics
     */
    // non-literal String log args (queueId from DB PK) wrapped by LogSanitizer.sanitize;
    // int args (statusCode, retryCount) carry no CRLF risk; find-sec-bugs cannot detect
    // user-defined sanitizer sink — suppress at method level consistent with OutboundRetryHandler
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "non-literal String log args wrapped by LogSanitizer.sanitize; "
                    + "find-sec-bugs cannot detect user-defined sanitizer; int args CRLF-safe")
    public CallbackFailureOutcome handleDeliveryFailure(final CallbackQueueEntity entity,
                                                        final int interfaceRetryCount,
                                                        final CallbackResult result) {
        final int newRetryCount = entity.getRetryCount() + 1;
        final String error = result == null ? null : result.error();

        // 4xx: non-retryable — go DLQ immediately regardless of remaining attempts
        if (isNonRetryable(result)) {
            entity.markDeadLetter(newRetryCount, error);
            repo.save(entity);
            publishDeadLetterEvent(entity, newRetryCount, error);
            LOG.warn("callback DEAD_LETTER (4xx non-retryable) queueId={} statusCode={}",
                    LogSanitizer.sanitize(entity.getQueueId()),
                    result.statusCode());
            return CallbackFailureOutcome.DEAD_LETTER;
        }

        // 5xx / IO: check against effective maxAttempts
        final int maxAttempts = interfaceRetryCount > 0
                ? interfaceRetryCount : props.retry().maxAttempts();

        if (newRetryCount >= maxAttempts) {
            entity.markDeadLetter(newRetryCount, error);
            repo.save(entity);
            publishDeadLetterEvent(entity, newRetryCount, error);
            LOG.warn("callback DEAD_LETTER (retries exhausted) queueId={} retryCount={}",
                    LogSanitizer.sanitize(entity.getQueueId()), newRetryCount);
            return CallbackFailureOutcome.DEAD_LETTER;
        }

        // Below max: RETRY with exp backoff
        final long shift = Math.min(newRetryCount, MAX_SHIFT_BITS);
        final long backoffMs = Math.min(
                props.retry().backoffMillis() << shift,
                props.retry().maxBackoffMillis());
        final LocalDateTime nextRetry = LocalDateTime.now(clock).plusNanos(backoffMs * 1_000_000L);
        entity.markRetry(newRetryCount, nextRetry, error);
        repo.save(entity);
        return CallbackFailureOutcome.RETRY;
    }

    /**
     * @param result HTTP 推送结果
     * @return {@code true} 当 statusCode 在 [400, 499] — 不可重试
     */
    private static boolean isNonRetryable(final CallbackResult result) {
        return result != null
                && result.statusCode() >= HTTP_CLIENT_ERROR_MIN
                && result.statusCode() <= HTTP_CLIENT_ERROR_MAX;
    }

    /**
     * 发布 {@link CallbackDeadLetterEvent}（T10 事件解耦）。两处 DEAD_LETTER 路径
     * （4xx 不可重试 / 重试耗尽）落库后调用，订阅方 {@code InAppNotificationListener}
     * （T12）异步写站内信，不影响 runner 批处理推进。
     *
     * @param entity        已落库的死信行
     * @param newRetryCount 进入死信时的累计重试次数
     * @param error         最后错误摘要
     */
    private void publishDeadLetterEvent(final CallbackQueueEntity entity,
                                        final int newRetryCount, final String error) {
        eventPublisher.publishEvent(new CallbackDeadLetterEvent(
                entity.getQueueId(),
                entity.getTargetInterfaceId(),
                entity.getMsgNo(),
                newRetryCount,
                error,
                LocalDateTime.now(clock)));
    }
}

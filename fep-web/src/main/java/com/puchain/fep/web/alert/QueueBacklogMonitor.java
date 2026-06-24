package com.puchain.fep.web.alert;

import com.puchain.fep.web.callback.repository.CallbackQueueRepository;
import com.puchain.fep.web.outbound.consumer.OutboundQueueRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * 队列积压监控器（DEF-B9-3 生产驱动）：周期采样 {@code callback_queue} +
 * {@code outbound_message_queue} 积压深度，超阈值时<b>边沿触发</b>——首次上穿发
 * {@link QueueBacklogEvent}，持续高位不重发，回落后 re-arm（muzhou 2026-06-24 D3 决策）。
 *
 * <p><b>生产驱动</b>：两队列由真实生产 poll 循环（{@code CallbackQueueRunner.poll()} /
 * {@code OutboundQueueConsumer.poll()}，均 {@code @Scheduled}）填充，本监控器的 {@code @Scheduled}
 * 采样即积压告警的生产入口（红线 tracking_table_hook_needs_production_driver 满足，非臆造 hook）。</p>
 *
 * <p><b>条件装配</b>：{@code @ConditionalOnProperty(fep.alert.queue-backlog.enabled, matchIfMissing=true)}
 * Bean 级开关，disabled 时 Bean 不创建、{@code @Scheduled} 不注册（镜像 {@code CallbackStaleReaper}）。</p>
 *
 * <p><b>并发</b>：{@code @Scheduled(fixedDelay)} 默认单线程串行执行，{@code alerting} {@link EnumMap}
 * 无并发问题。{@code @EnableScheduling} 已在 fep-web 全局启用，不在此重复。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "fep.alert.queue-backlog.enabled", havingValue = "true",
        matchIfMissing = true)
public class QueueBacklogMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(QueueBacklogMonitor.class);

    private final CallbackQueueRepository callbackRepo;
    private final OutboundQueueRepository outboundRepo;
    private final ApplicationEventPublisher publisher;
    private final QueueBacklogAlertProperties props;
    private final Clock clock;

    /** per-queue 边沿状态：true=当前处于「已告警」高位态（持续高位不重发，回落清零 re-arm）。 */
    private final Map<QueueBacklogQueue, Boolean> alerting = new EnumMap<>(QueueBacklogQueue.class);

    /**
     * @param callbackRepo callback 队列仓储（非空）
     * @param outboundRepo outbound 队列仓储（非空）
     * @param publisher    事件发布器（非空）
     * @param props        积压告警配置（非空）
     * @param clock        时钟来源（非空；生产系统 {@link Clock}，测试 fixed Clock）
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singletons stored by reference per container contract")
    public QueueBacklogMonitor(final CallbackQueueRepository callbackRepo,
            final OutboundQueueRepository outboundRepo, final ApplicationEventPublisher publisher,
            final QueueBacklogAlertProperties props, final Clock clock) {
        this.callbackRepo = Objects.requireNonNull(callbackRepo, "callbackRepo");
        this.outboundRepo = Objects.requireNonNull(outboundRepo, "outboundRepo");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.props = Objects.requireNonNull(props, "props");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.alerting.put(QueueBacklogQueue.CALLBACK, false);
        this.alerting.put(QueueBacklogQueue.OUTBOUND, false);
    }

    /** 周期采样两队列积压并按边沿触发告警。 */
    @Scheduled(fixedDelayString = "${fep.alert.queue-backlog.interval-ms:60000}")
    public void scan() {
        if (props.callbackEnabled()) {
            evaluateQueue(QueueBacklogQueue.CALLBACK, callbackRepo.countBacklog());
        }
        if (props.outboundEnabled()) {
            evaluateQueue(QueueBacklogQueue.OUTBOUND, outboundRepo.countBacklog());
        }
    }

    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "all log args are enum (queue) and primitive long (depth/threshold); "
                    + "no user-controlled String, no CRLF injection surface")
    private void evaluateQueue(final QueueBacklogQueue queue, final long depth) {
        final long threshold = props.threshold();
        final boolean wasAlerting = Boolean.TRUE.equals(alerting.get(queue));
        if (depth >= threshold) {
            if (!wasAlerting) {
                alerting.put(queue, true);
                LOG.warn("queue backlog crossed threshold: queue={} backlog={} threshold={}",
                        queue, depth, threshold);
                publisher.publishEvent(new QueueBacklogEvent(queue, depth, threshold,
                        LocalDateTime.now(clock)));
            }
        } else if (wasAlerting) {
            alerting.put(queue, false);
            LOG.info("queue backlog recovered below threshold: queue={} backlog={} threshold={}",
                    queue, depth, threshold);
        }
    }
}

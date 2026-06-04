package com.puchain.fep.web.requeststate;

import com.puchain.fep.common.util.LogSanitizer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 请求滞留检测定时任务（S2 request-state tracking）。
 *
 * <p>周期性扫描 {@link RequestStateLifecycle#SENT} 态超过可配 TTL 仍无结果返回的请求
 * （{@link RequestStateRepository#findStuck(Instant)}，已在 SQL 层排除
 * {@code correlation_blocked=true} 的结构性缺口行，见 {@link BlockedMessageTypes}），经单写者入口
 * {@link RequestStateService#markStuck(String)} 推进到 {@link RequestStateLifecycle#STUCK} 旁支终态，
 * 并递增 STUCK counter（{@code fep_request_state_stuck_total}）。</p>
 *
 * <p><b>调度</b>：{@code @Scheduled(fixedDelayString)} 上一次执行完成后固定延迟再触发（既有
 * {@code CallbackQueueRunner} / {@code OutboundQueueConsumer} 同款做法），延迟与 TTL 均从配置读取
 * （红线 6 无硬编码超时）。{@code @EnableScheduling} 不在本类重复声明——fep-web 已全局启用
 * （见 {@code DownloadTaskCleanupScheduler}），同既有 {@code CallbackQueueRunner} 约定（红线 9 风格一致）。</p>
 *
 * <p><b>单写者纪律</b>：reaper 不直接调用 {@code entity.markStuck()}/{@code repository.save}，而是经
 * {@link RequestStateService}（{@code REQUIRES_NEW} 短事务）逐行标记，保持状态机唯一写入入口
 * （T2 santa CONCERN-1）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class RequestStateReaper {

    private static final Logger LOG = LoggerFactory.getLogger(RequestStateReaper.class);

    static final String COUNTER_STUCK_TOTAL = "fep_request_state_stuck_total";

    private final RequestStateRepository repository;
    private final RequestStateService service;
    private final Counter stuckCounter;
    private final Duration stuckTtl;

    /**
     * Spring 构造器注入。
     *
     * @param repository request_state JPA repository（非空）
     * @param service    request-state 单写者状态机 Service（非空）
     * @param registry   Micrometer 注册中心（非空），用于注册 STUCK counter
     * @param stuckTtl   滞留判定 TTL（{@code fep.request-state.stuck-ttl}）：SENT 行 {@code updatedAt}
     *                   早于 {@code now - stuckTtl} 即视为滞留，非空
     */
    public RequestStateReaper(final RequestStateRepository repository,
                              final RequestStateService service,
                              final MeterRegistry registry,
                              @Value("${fep.request-state.stuck-ttl}") final Duration stuckTtl) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.service = Objects.requireNonNull(service, "service");
        this.stuckTtl = Objects.requireNonNull(stuckTtl, "stuckTtl");
        this.stuckCounter = Counter.builder(COUNTER_STUCK_TOTAL)
                .description("Total request_state rows marked STUCK by the reaper")
                .register(Objects.requireNonNull(registry, "registry"));
    }

    /**
     * 扫描并标记滞留请求行。
     *
     * <p>{@code findStuck(now - ttl)} 命中的每行经 {@link RequestStateService#markStuck(String)}
     * 标记为 {@link RequestStateLifecycle#STUCK}（已排除 correlation_blocked 行），命中即 counter++ 并
     * WARN 记录 correlation key 与报文号供 ops 排查。</p>
     */
    @Scheduled(fixedDelayString = "${fep.request-state.reaper.fixed-delay:60000}")
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "non-literal String log args wrapped by "
                    + "LogSanitizer.sanitize (find-sec-bugs cannot detect "
                    + "user-defined sanitizer)")
    public void sweep() {
        final Instant threshold = Instant.now().minus(stuckTtl);
        final List<RequestStateEntity> stuck = repository.findStuck(threshold);
        for (final RequestStateEntity entity : stuck) {
            final boolean marked = service.markStuck(entity.getCorrelationKey());
            if (marked) {
                stuckCounter.increment();
                LOG.warn("request_state STUCK detected: correlationKey={} messageType={} "
                                + "sentAt={}",
                        LogSanitizer.sanitize(entity.getCorrelationKey()),
                        LogSanitizer.sanitize(entity.getMessageType()),
                        entity.getSentAt());
            }
        }
    }
}

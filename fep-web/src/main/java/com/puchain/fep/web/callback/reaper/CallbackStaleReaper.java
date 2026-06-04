package com.puchain.fep.web.callback.reaper;

import com.puchain.fep.web.callback.config.CallbackQueueProperties;
import com.puchain.fep.web.callback.domain.CallbackQueueEntity;
import com.puchain.fep.web.callback.repository.CallbackQueueRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 回调队列僵尸 {@code SENDING} 行回收定时任务（Callback Phase 2b T13）。
 *
 * <p>周期性扫描 {@code claimedAt} 早于 {@code now - staleAfterSeconds} 仍停留在 {@code SENDING}
 * 的行（声领后实例崩溃/超时未推进，{@link CallbackQueueRepository#findStaleSending}），经
 * {@link CallbackQueueEntity#markAsStaleReclaim()} 回退到 {@code PENDING}（retryCount++、清空
 * claimedAt）使其重新可被 {@code claimBatch} 声领；retry handler 接管后超 maxAttempts 即转
 * {@code DEAD_LETTER} 并发布 DLQ 事件。每回收一行递增 counter（{@code fep_callback_reaper_reverted_total}）。
 * 参见 PRD v1.3 §5.5.3 回调可靠性（FR-INFRA-CALLBACK-LEASE）。</p>
 *
 * <p><b>条件装配</b>：{@code @ConditionalOnProperty(fep.callback.reaper.enabled, matchIfMissing=true)}
 * Bean 级开关 —— disabled 时 Bean 不创建、{@code @Scheduled} 不注册，避免内部短路造成的空跑噪声。</p>
 *
 * <p><b>调度</b>：{@code @Scheduled(fixedDelayString)} 上次执行完成后固定延迟再触发（同既有
 * {@code RequestStateReaper} / {@code CallbackQueueRunner} 约定）；{@code @EnableScheduling} 已在
 * fep-web 全局启用，不在本类重复声明。延迟与滞留窗均从配置读取（红线 6 无硬编码超时）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(name = "fep.callback.reaper.enabled", havingValue = "true",
        matchIfMissing = true)
public class CallbackStaleReaper {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackStaleReaper.class);

    static final String COUNTER_REVERTED_TOTAL = "fep_callback_reaper_reverted_total";

    private final CallbackQueueRepository repo;
    private final CallbackQueueProperties props;
    private final Clock clock;
    private final Counter revertedCounter;

    /**
     * Spring 构造器注入。
     *
     * @param repo     回调队列仓储（非空）
     * @param props    回调队列配置（读取 {@code reaper.staleAfterSeconds}，非空）
     * @param clock    时钟来源（生产注入系统 {@link Clock} bean，测试注入 fixed Clock，非空）
     * @param registry Micrometer 注册中心（非空），注册回收 counter
     */
    public CallbackStaleReaper(final CallbackQueueRepository repo,
            final CallbackQueueProperties props, final Clock clock, final MeterRegistry registry) {
        this.repo = Objects.requireNonNull(repo, "repo");
        this.props = Objects.requireNonNull(props, "props");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.revertedCounter = Counter.builder(COUNTER_REVERTED_TOTAL)
                .description("stale SENDING callback rows reverted to PENDING by the reaper")
                .register(Objects.requireNonNull(registry, "registry"));
    }

    /**
     * 扫描并回收僵尸 {@code SENDING} 行。
     *
     * <p>阈值 {@code now(clock) - staleAfterSeconds}；命中每行 markAsStaleReclaim + save + counter++，
     * 并 WARN 记录命中数与阈值供 ops 排查。无命中即静默返回（不记日志）。</p>
     */
    @Scheduled(fixedDelayString = "${fep.callback.reaper.interval-ms:60000}")
    @Transactional
    public void reap() {
        final LocalDateTime threshold = LocalDateTime.now(clock)
                .minusSeconds(props.reaper().staleAfterSeconds());
        final List<CallbackQueueEntity> stale = repo.findStaleSending(threshold);
        if (stale.isEmpty()) {
            return;
        }
        LOG.warn("callback reaper found {} stale SENDING rows older than {}", stale.size(), threshold);
        for (final CallbackQueueEntity entity : stale) {
            entity.markAsStaleReclaim();
            repo.save(entity);
            revertedCounter.increment();
        }
    }
}

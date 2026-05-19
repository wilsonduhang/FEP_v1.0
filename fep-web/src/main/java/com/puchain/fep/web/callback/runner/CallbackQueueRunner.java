package com.puchain.fep.web.callback.runner;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.domain.CallbackQueueEntity;
import com.puchain.fep.web.callback.domain.CallbackQueueStatus;
import com.puchain.fep.web.callback.http.CallbackHttpClient;
import com.puchain.fep.web.callback.http.CallbackResult;
import com.puchain.fep.web.callback.repository.CallbackQueueRepository;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import com.puchain.fep.web.submission.outputinterface.repository.SubOutputInterfaceRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 接口模式回调队列轮询调度器（Phase 1，模式 B）。
 *
 * <p>每隔 {@code fep.callback.poll-interval-ms}（默认 5000ms）触发 {@link #poll()}：</p>
 * <ol>
 *   <li>查询最多 50 条 PENDING 队列条目（criterion 4：{@code findTop50}）</li>
 *   <li>逐条调用 {@link #processOne(CallbackQueueEntity)}</li>
 *   <li>每行独立 {@code try/catch}：单行异常仅记 ERROR，不阻断同批其他行</li>
 * </ol>
 *
 * <p>{@code @Scheduled} 由 fep-web 已有的 {@code @EnableScheduling}
 * （见 {@code DownloadTaskCleanupScheduler} / {@code OutboundQueueConsumer}）启用，
 * 无需重复声明。</p>
 *
 * <p><strong>事务策略（P1）</strong>：每个 {@link CallbackQueueRepository#save} /
 * {@link SubOutputInterfaceRepository#save} 调用均在各自 Spring Data 隐式事务中提交。
 * {@code processOne} 未加 {@code @Transactional}，原因：{@link #poll()} 是 {@code @Scheduled}
 * 方法，调用 {@code processOne(this.processOne)} 为自调用，代理拦截器不生效；
 * P1 可接受 markDone 已提交而 callCount writeback 失败的最坏情况（dup-guard 幂等兜底）。
 * Phase 2 可引入 {@code processOneUseCase} bean 解耦自调用问题。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class CallbackQueueRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackQueueRunner.class);

    private final CallbackQueueRepository callbackQueueRepository;
    private final CallbackHttpClient httpClient;
    private final SubOutputInterfaceRepository subOutputInterfaceRepository;

    /**
     * Constructor 注入三项依赖（避免字段注入，便于测试）。
     *
     * @param callbackQueueRepository      回调队列 Repository，非空
     * @param httpClient                   HTTP 推送客户端，非空
     * @param subOutputInterfaceRepository 输出接口 Repository，非空
     */
    public CallbackQueueRunner(final CallbackQueueRepository callbackQueueRepository,
                               final CallbackHttpClient httpClient,
                               final SubOutputInterfaceRepository subOutputInterfaceRepository) {
        this.callbackQueueRepository = callbackQueueRepository;
        this.httpClient = httpClient;
        this.subOutputInterfaceRepository = subOutputInterfaceRepository;
    }

    /**
     * 周期性 poll 入口，由 Spring {@code TaskScheduler} 触发。
     *
     * <p>per-row 异常隔离：单行 {@code processOne} 抛出 RuntimeException 时仅记 ERROR，
     * 继续处理同批其余条目（镜像 {@code OutboundQueueConsumer} 模式）。</p>
     */
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "non-literal String log args wrapped by LogSanitizer.sanitize; "
                    + "find-sec-bugs cannot detect user-defined sanitizer")
    @Scheduled(fixedDelayString = "${fep.callback.poll-interval-ms:5000}",
            initialDelayString = "${fep.callback.poll-initial-delay-ms:5000}")
    public void poll() {
        final List<CallbackQueueEntity> pending =
                callbackQueueRepository.findTop50ByStatusOrderByCreateTimeAsc(CallbackQueueStatus.PENDING);
        if (pending.isEmpty()) {
            return;
        }
        for (final CallbackQueueEntity entity : pending) {
            try {
                processOne(entity);
            } catch (final RuntimeException e) {
                LOG.error("callback runner row failed queueId={}",
                        LogSanitizer.sanitize(entity.getQueueId()), e);
            }
        }
    }

    /**
     * 处理单条 PENDING 队列条目：查找目标接口 → HTTP POST → 更新状态。
     *
     * <p>目标接口不存在（criterion 5）：标记 FAILED，不抛出。
     * 2xx（criterion 2）：DONE + callCount+1 + lastCallTime。
     * 非 2xx / IO 异常（criterion 3）：FAILED + lastError。
     * P1 不重试（Phase 2 引入重试策略）。</p>
     *
     * @param entity PENDING 队列条目，非空
     */
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "non-literal String log args wrapped by LogSanitizer.sanitize "
                    + "(find-sec-bugs cannot detect user-defined sanitizer); "
                    + "int statusCode is primitive and CRLF-safe")
    void processOne(final CallbackQueueEntity entity) {
        final String interfaceId = entity.getTargetInterfaceId();
        final Optional<SubOutputInterface> opt = subOutputInterfaceRepository.findById(interfaceId);
        if (opt.isEmpty()) {
            // criterion 5: interface deleted/missing → FAILED, do not propagate
            entity.markFailed("interface not found");
            callbackQueueRepository.save(entity);
            LOG.warn("callback target not found queueId={} interfaceId={}",
                    LogSanitizer.sanitize(entity.getQueueId()),
                    LogSanitizer.sanitize(interfaceId));
            return;
        }

        final SubOutputInterface target = opt.get();
        final CallbackResult result = httpClient.post(target, entity.getPayloadJson());

        if (result.success()) {
            // criterion 2: mark DONE + increment callCount + update lastCallTime
            entity.markDone();
            callbackQueueRepository.save(entity);
            target.setCallCount(target.getCallCount() + 1L);
            target.setLastCallTime(LocalDateTime.now());
            subOutputInterfaceRepository.save(target);
            LOG.info("callback sent queueId={} interfaceId={} status={}",
                    LogSanitizer.sanitize(entity.getQueueId()),
                    LogSanitizer.sanitize(interfaceId),
                    result.statusCode());
        } else {
            // criterion 3: non-2xx or IO failure → FAILED, P1 no retry
            entity.markFailed(result.error());
            callbackQueueRepository.save(entity);
            LOG.warn("callback failed queueId={} interfaceId={} error={}",
                    LogSanitizer.sanitize(entity.getQueueId()),
                    LogSanitizer.sanitize(interfaceId),
                    LogSanitizer.sanitize(result.error()));
        }
    }
}

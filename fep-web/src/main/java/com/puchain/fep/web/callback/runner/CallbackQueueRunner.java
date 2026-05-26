package com.puchain.fep.web.callback.runner;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.config.CallbackQueueProperties;
import com.puchain.fep.web.callback.domain.CallbackQueueEntity;
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
 * 接口模式回调队列轮询调度器（Phase 2，模式 B）。
 *
 * <p>每隔 {@code fep.callback.poll-interval-ms}（默认 5000ms）触发 {@link #poll()}：</p>
 * <ol>
 *   <li>通过 {@code claimBatch} FOR UPDATE SKIP LOCKED 声领 ≤ {@code batchSize} 条
 *       PENDING/到期 RETRY 条目（多实例安全，镜像 {@code OutboundQueueConsumer}）</li>
 *   <li>逐条调用 {@link #processOne(String)}</li>
 *   <li>每行独立 {@code try/catch}：单行异常仅记 ERROR，不阻断同批其他行</li>
 * </ol>
 *
 * <p>{@code @Scheduled} 由 fep-web 已有的 {@code @EnableScheduling}
 * （见 {@code DownloadTaskCleanupScheduler} / {@code OutboundQueueConsumer}）启用，
 * 无需重复声明。</p>
 *
 * <p><strong>事务策略（P2）</strong>：每个 {@link CallbackQueueRepository#save} /
 * {@link SubOutputInterfaceRepository#save} 调用均在各自 Spring Data 隐式事务中提交。
 * {@code processOne} 未加 {@code @Transactional}，原因：{@link #poll()} 是 {@code @Scheduled}
 * 方法，调用 {@code processOne} 为自调用，代理拦截器不生效。</p>
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
    private final CallbackQueueProperties props;
    private final CallbackRetryHandler retryHandler;

    /**
     * Constructor 注入五项依赖（避免字段注入，便于测试）。
     *
     * @param callbackQueueRepository      回调队列 Repository，非空
     * @param httpClient                   HTTP 推送客户端，非空
     * @param subOutputInterfaceRepository 输出接口 Repository，非空
     * @param props                        队列配置（batchSize 等），非空
     * @param retryHandler                 投递失败处理器，非空
     */
    public CallbackQueueRunner(final CallbackQueueRepository callbackQueueRepository,
                               final CallbackHttpClient httpClient,
                               final SubOutputInterfaceRepository subOutputInterfaceRepository,
                               final CallbackQueueProperties props,
                               final CallbackRetryHandler retryHandler) {
        this.callbackQueueRepository = callbackQueueRepository;
        this.httpClient = httpClient;
        this.subOutputInterfaceRepository = subOutputInterfaceRepository;
        this.props = props;
        this.retryHandler = retryHandler;
    }

    /**
     * 周期性 poll 入口，由 Spring {@code TaskScheduler} 触发。
     *
     * <p>claimBatch FOR UPDATE SKIP LOCKED 声领当批 IDs；per-row 异常隔离：
     * 单行 {@code processOne} 抛出 RuntimeException 时仅记 ERROR，
     * 继续处理同批其余条目（镜像 {@code OutboundQueueConsumer} 模式）。</p>
     */
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "non-literal String log args wrapped by LogSanitizer.sanitize; "
                    + "find-sec-bugs cannot detect user-defined sanitizer")
    @Scheduled(fixedDelayString = "${fep.callback.poll-interval-ms:5000}",
            initialDelayString = "${fep.callback.poll-initial-delay-ms:5000}")
    public void poll() {
        final List<String> ids = callbackQueueRepository.claimBatch(props.batchSize());
        if (ids.isEmpty()) {
            return;
        }
        for (final String id : ids) {
            try {
                processOne(id);
            } catch (final RuntimeException e) {
                LOG.error("callback runner row failed queueId={}", LogSanitizer.sanitize(id), e);
            }
        }
    }

    /**
     * 处理单条队列条目：findById → markSending → 查找目标接口 → HTTP POST → 更新状态。
     *
     * <p>目标接口不存在：标记 FAILED，不抛出（保留 Phase 1 行为）。
     * 2xx：DONE + callCount+1 + lastCallTime。
     * 非 2xx / IO 异常：委托 {@link CallbackRetryHandler#handleDeliveryFailure}
     * （决定 RETRY 或 DEAD_LETTER）。</p>
     *
     * @param queueId queue_id 主键，非空
     */
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "non-literal String log args wrapped by LogSanitizer.sanitize "
                    + "(find-sec-bugs cannot detect user-defined sanitizer); int statusCode CRLF-safe")
    void processOne(final String queueId) {
        final Optional<CallbackQueueEntity> entOpt = callbackQueueRepository.findById(queueId);
        if (entOpt.isEmpty()) {
            return; // 已被其它实例处理或清理，跳过
        }
        final CallbackQueueEntity entity = entOpt.get();
        entity.markSending();
        callbackQueueRepository.save(entity);

        final String interfaceId = entity.getTargetInterfaceId();
        final Optional<SubOutputInterface> opt = subOutputInterfaceRepository.findById(interfaceId);
        if (opt.isEmpty()) {
            entity.markFailed("interface not found");
            callbackQueueRepository.save(entity);
            LOG.warn("callback target not found queueId={} interfaceId={}",
                    LogSanitizer.sanitize(entity.getQueueId()), LogSanitizer.sanitize(interfaceId));
            return;
        }

        final SubOutputInterface target = opt.get();
        final CallbackResult result = httpClient.post(target, entity.getPayloadJson());

        if (result.success()) {
            entity.markDone();
            callbackQueueRepository.save(entity);
            target.setCallCount(target.getCallCount() + 1L);
            target.setLastCallTime(LocalDateTime.now());
            subOutputInterfaceRepository.save(target);
            LOG.info("callback sent queueId={} interfaceId={} status={}",
                    LogSanitizer.sanitize(entity.getQueueId()),
                    LogSanitizer.sanitize(interfaceId), result.statusCode());
        } else {
            retryHandler.handleDeliveryFailure(entity, target.getRetryCount(), result);
            LOG.warn("callback delivery failed queueId={} interfaceId={} error={}",
                    LogSanitizer.sanitize(entity.getQueueId()),
                    LogSanitizer.sanitize(interfaceId), LogSanitizer.sanitize(result.error()));
        }
    }
}
